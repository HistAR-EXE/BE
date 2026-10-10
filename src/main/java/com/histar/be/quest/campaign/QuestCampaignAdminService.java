package com.histar.be.quest.campaign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.quest.narrative.NarrativeInvestigationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestCampaignAdminService {
    private final QuestCampaignRepository repository;
    private final QuestCampaignAuditRepository audits;
    private final ObjectMapper objectMapper;
    private final NarrativeInvestigationRepository investigations;

    @Transactional
    public QuestCampaignResponse create(UUID actor, QuestCampaignRequest request) {
        validate(request, null);
        QuestCampaign campaign = new QuestCampaign();
        campaign.setId(UUID.randomUUID());
        apply(campaign, request);
        campaign.setEnabled(false);
        campaign = repository.save(campaign);
        audit(actor, campaign, "CREATED", null, null);
        return QuestCampaignResponse.from(campaign);
    }

    @Transactional
    public QuestCampaignResponse update(UUID actor, UUID id, QuestCampaignRequest request) {
        validate(request, id);
        QuestCampaign campaign = find(id);
        boolean wasPublished = campaign.isEnabled();
        apply(campaign, request);
        campaign.setEnabled(wasPublished && request.enabled());
        campaign = repository.save(campaign);
        audit(actor, campaign, "UPDATED", null, null);
        return QuestCampaignResponse.from(campaign);
    }

    @Transactional
    public QuestCampaignResponse setPublished(UUID actor, UUID id, boolean published) {
        QuestCampaign campaign = find(id);
        if (published) {
            validatePublishGate(campaign);
        }
        campaign.setEnabled(published);
        campaign = repository.save(campaign);
        audit(actor, campaign, published ? "PUBLISHED" : "UNPUBLISHED", null, null);
        return QuestCampaignResponse.from(campaign);
    }

    @Transactional
    public QuestCampaignResponse updatePilotGate(UUID actor, UUID id, QuestCampaignGateRequest request) {
        QuestCampaign campaign = find(id);
        if (request.releaseDecision() == ReleaseDecision.APPROVED && request.pilotStatus() != PilotStatus.PASSED) {
            throw new BusinessRuleException("Release APPROVED cần pilot status PASSED");
        }
        campaign.setPilotStatus(request.pilotStatus());
        campaign.setReleaseDecision(request.releaseDecision());
        campaign.setReleaseNote(request.note() == null ? null : request.note().trim());
        campaign.setReleaseDecidedBy(actor);
        campaign.setReleaseDecidedAt(Instant.now());
        campaign = repository.save(campaign);
        audit(actor, campaign, "PILOT_GATE_UPDATED", null, request.note());
        return QuestCampaignResponse.from(campaign);
    }

    @Transactional(readOnly = true)
    public List<QuestCampaignAuditResponse> history(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Quest campaign not found");
        }
        return audits.findByCampaignIdOrderByOccurredAtDesc(id).stream()
                .map(QuestCampaignAuditResponse::from)
                .toList();
    }

    @Transactional
    public QuestCampaignResponse rollback(UUID actor, UUID id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("Rollback cần lý do");
        }
        QuestCampaign campaign = find(id);
        campaign.setEnabled(false);
        campaign.setReleaseDecision(ReleaseDecision.ROLLED_BACK);
        campaign.setReleaseNote(reason.trim());
        campaign = repository.save(campaign);
        audit(actor, campaign, "ROLLED_BACK", reason.trim(), reason.trim());
        return QuestCampaignResponse.from(campaign);
    }

    private QuestCampaign find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Quest campaign not found"));
    }

    private void validate(QuestCampaignRequest request, UUID current) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new BusinessRuleException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        boolean overlap = repository.findByQuestIdAndCampaignTypeAndStartsAtLessThanAndEndsAtGreaterThan(
                        request.questId(), request.type(), request.endsAt(), request.startsAt()).stream()
                .anyMatch(c -> !Objects.equals(c.getId(), current));
        if (overlap) {
            throw new BusinessRuleException("Campaign cùng quest/type không được chồng thời gian");
        }
    }

    private void apply(QuestCampaign campaign, QuestCampaignRequest request) {
        campaign.setQuestId(request.questId());
        campaign.setCampaignType(request.type());
        campaign.setTitle(request.title().trim());
        campaign.setStartsAt(request.startsAt());
        campaign.setEndsAt(request.endsAt());
    }

    private void validatePublishGate(QuestCampaign campaign) {
        if (("DAILY".equals(campaign.getCampaignType()) || "WEEKLY".equals(campaign.getCampaignType()))
                && (campaign.getPilotStatus() != PilotStatus.PASSED
                || campaign.getReleaseDecision() != ReleaseDecision.APPROVED)) {
            throw new BusinessRuleException("Daily/weekly chỉ mở sau pilot gate đạt và release decision APPROVED");
        }
        boolean approvedContent = investigations.findByQuestId(campaign.getQuestId())
                .map(i -> "APPROVED".equals(i.getReviewStatus()) && i.getReviewedAt() != null)
                .orElse(false);
        if (!approvedContent) {
            throw new BusinessRuleException("Quest content phải được chuyên gia duyệt trước khi publish campaign");
        }
    }

    /** Used by teacher assignments and cohort-only reads, not just admin publish. */
    public void requireAvailableNow(QuestCampaign campaign, Instant now) {
        if (!campaign.isEnabled() || campaign.getPilotStatus() != PilotStatus.PASSED
                || campaign.getReleaseDecision() != ReleaseDecision.APPROVED
                || now.isBefore(campaign.getStartsAt()) || !now.isBefore(campaign.getEndsAt())) {
            throw new BusinessRuleException("Campaign chưa đạt release gate hoặc không nằm trong thời gian hoạt động");
        }
    }

    private void audit(UUID actor, QuestCampaign campaign, String action, String rollbackReason, String note) {
        try {
            audits.save(QuestCampaignAudit.builder()
                    .campaignId(campaign.getId())
                    .action(action)
                    .actorId(actor)
                    .occurredAt(Instant.now())
                    .rollbackReason(rollbackReason)
                    .detailJson(auditDetail(campaign, note))
                    .build());
        } catch (Exception exception) {
            throw new BusinessRuleException("Không thể lưu audit campaign");
        }
    }

    private String auditDetail(QuestCampaign campaign, String note) throws Exception {
        ObjectNode detail = objectMapper.createObjectNode();
        detail.put("campaignId", campaign.getId().toString());
        detail.put("questId", campaign.getQuestId().toString());
        detail.put("type", campaign.getCampaignType());
        detail.put("title", campaign.getTitle());
        detail.put("enabled", campaign.isEnabled());
        detail.put("pilotStatus", campaign.getPilotStatus().name());
        detail.put("releaseDecision", campaign.getReleaseDecision().name());
        detail.put("startsAt", campaign.getStartsAt().toString());
        detail.put("endsAt", campaign.getEndsAt().toString());
        if (note != null) {
            detail.put("note", note);
        }
        return objectMapper.writeValueAsString(detail);
    }
}
