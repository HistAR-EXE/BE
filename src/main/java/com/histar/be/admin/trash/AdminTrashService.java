package com.histar.be.admin.trash;

import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.quest.narrative.NarrativeInvestigationRepository;
import com.histar.be.quest.repository.QuestRepository;
import java.util.UUID;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminTrashService {
    private final QuestRepository quests;
    private final NarrativeInvestigationRepository investigations;
    private final AdminAuditService audit;

    public void softDeleteQuest(UUID questId, UUID actorId, AdminDeleteRequest request) {
        if (investigations.findByQuestIdAndEnabledTrue(questId).isPresent()) {
            throw new BusinessRuleException("Không thể xóa quest còn pilot active");
        }
        String reason = normalizeReason(request);
        var quest = quests.findById(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest not found: " + questId));
        if (!java.util.Objects.equals(quest.getRowVersion(), request.rowVersion())) {
            throw new BusinessRuleException("Nội dung đã bị thay đổi; hãy tải lại trước khi xóa");
        }
        if (quest.getDeletedAt() != null) {
            return;
        }
        Long oldVersion = quest.getRowVersion();
        quest.setDeletedAt(Instant.now());
        quest.setDeletedBy(actorId);
        quest.setDeleteReason(reason);
        quests.save(quest);
        audit.append(new AdminAuditCommand(actorId, "QUEST", questId, "SOFT_DELETED", oldVersion,
                oldVersion == null ? null : oldVersion + 1, reason, request.idempotencyKey(),
                Map.of("title", quest.getTitle() == null ? "" : quest.getTitle())));
    }

    public void restoreQuest(UUID questId, UUID actorId, AdminRestoreRequest request) {
        var quest = quests.findById(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest not found: " + questId));
        if (!java.util.Objects.equals(quest.getRowVersion(), request.rowVersion())) {
            throw new BusinessRuleException("Nội dung đã bị thay đổi; hãy tải lại trước khi khôi phục");
        }
        if (quest.getDeletedAt() == null) {
            return;
        }
        Long oldVersion = quest.getRowVersion();
        quest.setDeletedAt(null);
        quest.setDeletedBy(null);
        quest.setDeleteReason(null);
        quests.save(quest);
        audit.append(new AdminAuditCommand(actorId, "QUEST", questId, "RESTORED", oldVersion,
                oldVersion == null ? null : oldVersion + 1, null, request.idempotencyKey(),
                Map.of("title", quest.getTitle() == null ? "" : quest.getTitle())));
    }

    private String normalizeReason(AdminDeleteRequest request) {
        String preset = request.reasonPreset() == null ? "" : request.reasonPreset().trim();
        if (!java.util.Set.of("Sai nguồn", "Lỗi kỹ thuật", "Trùng nội dung", "Yêu cầu pháp lý", "Khác").contains(preset)) {
            throw new BusinessRuleException("Lý do xóa không hợp lệ");
        }
        String detail = request.reasonDetail() == null ? "" : request.reasonDetail().trim();
        if ("Khác".equals(preset) && detail.length() < 10) {
            throw new BusinessRuleException("Lý do khác cần ít nhất 10 ký tự");
        }
        return "Khác".equals(preset) ? detail : preset;
    }
}
