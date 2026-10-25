package com.histar.be.rag.service;

import com.histar.be.chat.dto.ChatHistoryTurn;
import com.histar.be.chat.dto.ChatSource;
import com.histar.be.chat.service.ChatLlmClient;
import com.histar.be.chat.service.RagServiceException;
import com.histar.be.rag.config.RagProperties;
import com.histar.be.rag.dto.RagChatAnswer;
import com.histar.be.rag.dto.RagChatRequest;
import com.histar.be.rag.dto.RagStreamChunk;
import com.histar.be.rag.dto.RetrievalResult;
import com.histar.be.rag.dto.RetrievedChunk;
import com.histar.be.rag.entity.ChatQualityLog;
import com.histar.be.rag.repository.ChatQualityLogRepository;
import com.histar.be.observability.ProductionMetrics;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * RAG chat path: retrieve verified chunks, prompt with numbered sources, require [n] citations in the answer and
 * soft-refuse otherwise. Returns {@link Optional#empty()} when RAG is disabled, unavailable or found no chunk so the
 * caller can fall back to the legacy chat pipeline.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagChatService {

    public static final String SOFT_REFUSE_REPLY =
            "Mình chưa tìm thấy tư liệu đã xác minh để trả lời chắc chắn câu hỏi này. "
                    + "Bạn thử hỏi cụ thể hơn về trạm đang tham quan hoặc tham khảo tài liệu chính thức của Ban quản lý di tích nhé.";

    private static final Pattern CITATION = Pattern.compile("\\[(\\d{1,2})]");
    private static final int MAX_HISTORY_TURNS = 6;

    private final PgVectorRagService ragService;
    private final SemanticCacheService semanticCache;
    private final ChatLlmClient chatLlmClient;
    private final ChatQualityLogRepository qualityLogRepository;
    private final RagProperties ragProperties;
    private final ProductionMetrics productionMetrics;

    public boolean isEnabled() {
        return ragProperties.isEnabled();
    }

    public Optional<RagChatAnswer> tryAnswer(RagChatRequest request) {
        if (!ragProperties.isEnabled() || !request.isHeritageOnly()) {
            return Optional.empty();
        }
        long startNanos = System.nanoTime();
        String stationCode = blankToNull(request.stationCode());
        RetrievalResult retrieval;
        try {
            retrieval = ragService.retrieve(request.question(), stationCode, request.siteCode());
        } catch (RuntimeException ex) {
            log.warn("requestId={} semantic_retrieval_failed", org.slf4j.MDC.get("requestId"));
            return Optional.empty();
        }
        List<RetrievedChunk> chunks = retrieval.chunks();
        if (chunks.isEmpty()) {
            logQuality(request, 0, false, startNanos);
            return Optional.empty();
        }

        try {
            Optional<String> cached =
                    semanticCache.lookup(retrieval.vectorLiteral(), stationCode, request.personaKey());
            if (cached.isPresent()) {
                List<ChatSource> sources = citedSources(cached.get(), chunks);
                logQuality(request, chunks.size(), !sources.isEmpty(), startNanos);
                productionMetrics.recordRagCitation(!sources.isEmpty());
                return Optional.of(new RagChatAnswer(cached.get(), sources, false, true));
            }

            String raw = chatLlmClient.generate(buildPrompt(request, chunks));
            String reply = normalizeCitations(raw, chunks.size());
            List<ChatSource> sources = citedSources(reply, chunks);
            boolean cited = !sources.isEmpty();
            logQuality(request, chunks.size(), cited, startNanos);
            productionMetrics.recordRagCitation(cited);

            if (!cited) {
                return Optional.of(new RagChatAnswer(SOFT_REFUSE_REPLY, List.of(), true, false));
            }
            semanticCache.store(retrieval.vectorLiteral(), stationCode, request.personaKey(), reply);
            return Optional.of(new RagChatAnswer(reply, sources, false, false));
        } catch (RuntimeException ex) {
            log.warn("requestId={} semantic_generation_failed", org.slf4j.MDC.get("requestId"));
            return Optional.empty();
        }
    }

    /**
     * Uses the provider's SSE stream but releases text only through a valid numbered citation.
     * Consequently an uncited/hallucinated answer never reaches the browser before soft refusal.
     */
    public Optional<RagChatAnswer> tryAnswerStreaming(RagChatRequest request, Consumer<RagStreamChunk> publisher) {
        if (!ragProperties.isEnabled() || !request.isHeritageOnly()) return Optional.empty();
        RetrievalResult retrieval;
        try {
            retrieval = ragService.retrieve(request.question(), blankToNull(request.stationCode()), request.siteCode());
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
        List<RetrievedChunk> chunks = retrieval.chunks();
        if (chunks.isEmpty()) return Optional.empty();
        String requestId = org.slf4j.MDC.get("requestId");
        boolean[] published = {false};
        RuntimeException[] publicationFailure = {null};
        Consumer<RagStreamChunk> publish = chunk -> {
            try {
                publisher.accept(chunk);
                published[0] = true;
            } catch (RuntimeException ex) {
                publicationFailure[0] = ex;
                throw ex;
            }
        };
        try {
            Optional<String> cached = semanticCache.lookup(retrieval.vectorLiteral(), blankToNull(request.stationCode()), request.personaKey());
            if (cached.isPresent()) {
                List<ChatSource> sources = citedSources(cached.get(), chunks);
                if (!sources.isEmpty()) publish.accept(new RagStreamChunk(cached.get(), sources));
                return Optional.of(new RagChatAnswer(sources.isEmpty() ? SOFT_REFUSE_REPLY : cached.get(), sources, sources.isEmpty(), true));
            }
            StringBuilder raw = new StringBuilder();
            int[] emitted = {0};
            chatLlmClient.stream(buildPrompt(request, chunks), token -> {
                raw.append(token);
                int safeEnd = lastValidCitationEnd(raw, chunks.size());
                if (safeEnd > emitted[0]) {
                    String safe = raw.substring(emitted[0], safeEnd);
                    publish.accept(new RagStreamChunk(safe, citedSources(safe, chunks)));
                    emitted[0] = safeEnd;
                }
            });
            String reply = normalizeCitations(raw.toString(), chunks.size());
            List<ChatSource> sources = citedSources(reply, chunks);
            if (sources.isEmpty()) return Optional.of(new RagChatAnswer(SOFT_REFUSE_REPLY, List.of(), true, false));
            String safeReply = reply.substring(0, Math.min(reply.length(), emitted[0])).trim();
            if (safeReply.isBlank()) safeReply = reply;
            semanticCache.store(retrieval.vectorLiteral(), blankToNull(request.stationCode()), request.personaKey(), safeReply);
            return Optional.of(new RagChatAnswer(safeReply, sources, false, false));
        } catch (RuntimeException ex) {
            if (publicationFailure[0] != null) throw publicationFailure[0];
            log.warn("requestId={} semantic_stream_failed", requestId);
            if (published[0]) throw new RagServiceException(false, true, requestId);
            return Optional.empty();
        }
    }

    private static int lastValidCitationEnd(CharSequence text, int chunkCount) {
        Matcher matcher = CITATION.matcher(text);
        int end = 0;
        while (matcher.find()) {
            int number = Integer.parseInt(matcher.group(1));
            if (number >= 1 && number <= chunkCount) end = matcher.end();
        }
        return end;
    }

    String buildPrompt(RagChatRequest request, List<RetrievedChunk> chunks) {
        String place = request.locationName() != null && !request.locationName().isBlank()
                ? request.locationName()
                : "di tích lịch sử";
        StringBuilder sb = new StringBuilder();
        if (request.personaPrompt() != null && !request.personaPrompt().isBlank()) {
            sb.append(request.personaPrompt().trim()).append("\n\n");
        }
        sb.append("Bạn là hướng dẫn viên lịch sử tại ").append(place).append(".\n\n")
                .append("QUY TẮC BẮT BUỘC:\n")
                .append("1. CHỈ dùng thông tin trong phần TƯ LIỆU ĐÃ XÁC MINH bên dưới; tuyệt đối không bịa số liệu, ngày tháng, sự kiện.\n")
                .append("2. Sau mỗi ý lấy từ tư liệu, gắn chú thích nguồn dạng [1], [2] đúng với số thứ tự tư liệu.\n")
                .append("3. Nếu tư liệu không đủ để trả lời, nói rõ là chưa có tư liệu xác minh (không gắn chú thích).\n")
                .append("4. Giữ giọng nhân vật, trả lời ngắn gọn bằng tiếng Việt.\n\n")
                .append("TƯ LIỆU ĐÃ XÁC MINH:\n");
        if (request.stationContext() != null) {
            sb.append("\nNGỮ CẢNH TRẠM ĐANG XEM:\n")
                    .append("Người dùng đang xem ")
                    .append(request.stationContext().stationCode())
                    .append(" · ")
                    .append(request.stationContext().stationName())
                    .append(". Đây là ngữ cảnh điều hướng, không phải bằng chứng người dùng đã check-in.\n");
            if (request.stationContext().summary() != null && !request.stationContext().summary().isBlank()) {
                sb.append(request.stationContext().summary().trim()).append('\n');
            }
        }
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk c = chunks.get(i);
            sb.append('[').append(i + 1).append("] ").append(c.sourceTitle());
            if (c.era() != null && !c.era().isBlank()) {
                sb.append(" (").append(c.era()).append(')');
            }
            sb.append(": ").append(c.content().trim()).append('\n');
        }
        List<ChatHistoryTurn> history = request.history() == null ? List.of() : request.history();
        if (!history.isEmpty()) {
            sb.append("\nLịch sử hội thoại gần đây:\n");
            int start = Math.max(0, history.size() - MAX_HISTORY_TURNS);
            for (int i = start; i < history.size(); i++) {
                ChatHistoryTurn t = history.get(i);
                sb.append("user".equals(t.role()) ? "Người dùng" : "Bạn").append(": ").append(t.content()).append('\n');
            }
        }
        sb.append("\nCâu hỏi của người dùng: ").append(request.question().trim());
        return sb.toString();
    }

    /** Removes citation markers that point outside 1..chunkCount (hallucinated numbers). */
    static String normalizeCitations(String reply, int chunkCount) {
        if (reply == null) {
            return "";
        }
        Matcher m = CITATION.matcher(reply);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            int n = Integer.parseInt(m.group(1));
            m.appendReplacement(out, n >= 1 && n <= chunkCount ? Matcher.quoteReplacement(m.group()) : "");
        }
        m.appendTail(out);
        return out.toString().trim();
    }

    /** Sources for the distinct valid citations, in order of first appearance. */
    static List<ChatSource> citedSources(String reply, List<RetrievedChunk> chunks) {
        Set<Integer> indexes = new LinkedHashSet<>();
        Matcher m = CITATION.matcher(reply == null ? "" : reply);
        while (m.find()) {
            int n = Integer.parseInt(m.group(1));
            if (n >= 1 && n <= chunks.size()) {
                indexes.add(n);
            }
        }
        return indexes.stream()
                .map(n -> chunks.get(n - 1))
                .map(c -> new ChatSource(c.sourceTitle(), excerpt(c.content()), null))
                .toList();
    }

    private static String excerpt(String content) {
        String s = content == null ? "" : content.trim();
        return s.length() > 160 ? s.substring(0, 157) + "..." : s;
    }

    private void logQuality(RagChatRequest request, int chunksUsed, boolean hadCitation, long startNanos) {
        try {
            long ms = (System.nanoTime() - startNanos) / 1_000_000L;
            qualityLogRepository.save(ChatQualityLog.builder()
                    .questionHash(hashQuestion(request.question()))
                    .stationCode(blankToNull(request.stationCode()))
                    .chunksUsed(chunksUsed)
                    .hadCitation(hadCitation)
                    .latencyMs((int) Math.min(ms, Integer.MAX_VALUE))
                    .createdAt(Instant.now())
                    .build());
        } catch (RuntimeException ex) {
            log.warn("requestId={} chat_quality_log_write_failed", org.slf4j.MDC.get("requestId"));
        }
    }

    static String hashQuestion(String question) {
        String normalized = question == null ? "" : question.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
