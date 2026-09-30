package com.histar.be.rag.service;

import com.histar.be.chat.dto.ChatHistoryTurn;
import com.histar.be.chat.dto.ChatSource;
import com.histar.be.chat.service.ChatLlmClient;
import com.histar.be.rag.config.RagProperties;
import com.histar.be.rag.dto.RagChatAnswer;
import com.histar.be.rag.dto.RagChatRequest;
import com.histar.be.rag.dto.RetrievalResult;
import com.histar.be.rag.dto.RetrievedChunk;
import com.histar.be.rag.entity.ChatQualityLog;
import com.histar.be.rag.repository.ChatQualityLogRepository;
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

    public boolean isEnabled() {
        return ragProperties.isEnabled();
    }

    public Optional<RagChatAnswer> tryAnswer(RagChatRequest request) {
        if (!ragProperties.isEnabled()) {
            return Optional.empty();
        }
        long startNanos = System.nanoTime();
        String stationCode = blankToNull(request.stationCode());
        RetrievalResult retrieval;
        try {
            retrieval = ragService.retrieve(request.question(), stationCode, request.siteCode());
        } catch (RuntimeException ex) {
            log.warn("RAG retrieval failed, falling back to legacy chat: {}", ex.getMessage());
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
                return Optional.of(new RagChatAnswer(cached.get(), sources, false, true));
            }

            String raw = chatLlmClient.generate(buildPrompt(request, chunks));
            String reply = normalizeCitations(raw, chunks.size());
            List<ChatSource> sources = citedSources(reply, chunks);
            boolean cited = !sources.isEmpty();
            logQuality(request, chunks.size(), cited, startNanos);

            if (!cited) {
                return Optional.of(new RagChatAnswer(SOFT_REFUSE_REPLY, List.of(), true, false));
            }
            semanticCache.store(retrieval.vectorLiteral(), stationCode, request.personaKey(), reply);
            return Optional.of(new RagChatAnswer(reply, sources, false, false));
        } catch (RuntimeException ex) {
            log.warn("RAG generation failed, falling back to legacy chat: {}", ex.getMessage());
            return Optional.empty();
        }
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
            log.warn("chat_quality_log write failed: {}", ex.getMessage());
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
