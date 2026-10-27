package com.histar.be.organization.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.config.HistarAppProperties;
import com.histar.be.config.HistarMailProperties;
import com.histar.be.config.TestHookProperties;
import com.histar.be.mail.EmailDeliveryException;
import com.histar.be.mail.HistarEmailService;
import com.histar.be.organization.dto.OrgInviteCodeResponse;
import com.histar.be.organization.dto.OrgInviteEmailRequest;
import com.histar.be.organization.dto.OrgEmailInvitationResponse;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrgInviteEmailService {

    private final OrgInviteService orgInviteService;
    private final OrgAccessService orgAccessService;
    private final OrganizationRepository organizationRepository;
    private final ProfileRepository profileRepository;
    private final HistarEmailService histarEmailService;
    private final HistarAppProperties appProperties;
    private final HistarMailProperties mailProperties;
    private final TestHookProperties testHookProperties;
    private final OrgEmailInvitationService emailInvitations;
    private final OrganizationEntitlementService entitlements;
    private final jakarta.persistence.EntityManager entityManager;

    @Transactional
    public OrgEmailInvitationResponse sendInviteEmail(UUID orgId, UUID teacherUserId, OrgInviteEmailRequest request) {
        entityManager.flush();
        Profile teacher = profileRepository.findByIdForUpdate(teacherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        entityManager.refresh(teacher, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        orgAccessService.requireOrgAccess(orgId);
        entitlements.requireCapability(teacherUserId, orgId, "manageMembers");
        Organization org = organizationRepository
                .findByIdForUpdate(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        OrgInviteCodeResponse invite = org.getInviteCode() == null || org.getInviteCode().isBlank()
                ? orgInviteService.generateInviteCode(orgId)
                : orgInviteService.getInviteCode(orgId);

        String inviteUrl = invite.inviteUrl();
        String studentName = request.studentName() != null && !request.studentName().isBlank()
                ? request.studentName().trim()
                : "bạn";
        String teacherName = teacher.getDisplayName() != null && !teacher.getDisplayName().isBlank()
                ? teacher.getDisplayName()
                : "Giáo viên";

        String deliveryStatus = dispatchEmail(
                request.studentEmail().trim(),
                studentName,
                teacherName,
                org.getName(),
                invite.inviteCode(),
                inviteUrl);
        return emailInvitations.record(orgId, teacherUserId, request.studentEmail(), deliveryStatus);
    }

    private String dispatchEmail(
            String to,
            String studentName,
            String teacherName,
            String orgName,
            String inviteCode,
            String inviteUrl) {
        String subject = "Mời tham gia lớp " + orgName + " trên TimeLens";
        String html = buildHtml(studentName, teacherName, orgName, inviteCode, inviteUrl);

        if (!mailProperties.isEnabled()) {
            log.info("Mail disabled — org invite for {}: code={} url={}", to, inviteCode, inviteUrl);
            return "LOCAL_ONLY";
        }
        if (testHookProperties.isEnabled()) {
            log.info("Test hooks enabled — skip mail; org invite for {}: code={} url={}", to, inviteCode, inviteUrl);
            return "LOCAL_ONLY";
        }
        try {
            histarEmailService.sendHtml(to, subject, html);
            return "SENT";
        } catch (EmailDeliveryException ex) {
            throw new BusinessRuleException("Không gửi được email mời. Thử lại sau.");
        }
    }

    private String buildHtml(
            String studentName, String teacherName, String orgName, String inviteCode, String inviteUrl) {
        String link = inviteUrl != null && !inviteUrl.isBlank()
                ? inviteUrl
                : appProperties.getFrontendUrl().replaceAll("/$", "") + "/join?code=" + inviteCode;
        return """
                <div style="font-family:Segoe UI,Arial,sans-serif;line-height:1.6;color:#1a1a1a;max-width:560px">
                  <p>Xin chào <strong>%s</strong>,</p>
                  <p><strong>%s</strong> mời bạn tham gia lớp <strong>%s</strong> trên TimeLens — nền tảng học lịch sử AR.</p>
                  <p style="margin:24px 0">
                    <a href="%s" style="background:#0877ff;color:#fff;padding:12px 24px;border-radius:8px;text-decoration:none;font-weight:bold">
                      Tham gia lớp ngay
                    </a>
                  </p>
                  <p>Mã mời 6 ký tự: <strong style="letter-spacing:2px">%s</strong></p>
                  <ol>
                    <li>Đăng ký tài khoản miễn phí tại TimeLens (nếu chưa có).</li>
                    <li>Đăng nhập bằng đúng email được mời và xác thực email để nhận quyền Premium theo tổ chức.</li>
                    <li>Nếu có nhiều lời mời, bấm liên kết trên để chọn lớp.</li>
                    <li>Bắt đầu khám phá quest và chat AI cùng lớp.</li>
                  </ol>
                  <p style="color:#666;font-size:13px">Liên kết hết hạn sau 7 ngày. Nếu bạn không biết người gửi, hãy bỏ qua email này.</p>
                </div>
                """
                .formatted(escape(studentName), escape(teacherName), escape(orgName), escape(link), escape(inviteCode));
    }

    private static String escape(String value) {
        return org.springframework.web.util.HtmlUtils.htmlEscape(value == null ? "" : value);
    }
}
