package com.histar.be.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.admin.dto.CreateAdminUserRequest;
import com.histar.be.admin.dto.UpdateUserRoleRequest;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ConflictException;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.config.HistarAppProperties;
import com.histar.be.config.HistarMailProperties;
import com.histar.be.mail.HistarEmailService;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock private ProfileRepository profiles;
    @Mock private CurrentUserAccessor currentUser;
    @Mock private AdminAuditService audit;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private HistarEmailService emailService;
    @Mock private HistarAppProperties appProperties;
    @Mock private HistarMailProperties mailProperties;

    private AdminUserService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserService(
                profiles, currentUser, audit, passwordEncoder, emailService, appProperties, mailProperties);
    }

    @Test
    void listsUsersWithSearchFiltersAndServerPagination() {
        Profile match = Profile.builder()
                .id(UUID.randomUUID())
                .email("lan@example.com")
                .displayName("Lan Nguyen")
                .role("TEACHER")
                .tier("PREMIUM")
                .emailVerified(true)
                .build();
        when(profiles.findAdminUsers(
                        eq("lan"), eq("TEACHER"), eq("PREMIUM"), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of(match), PageRequest.of(1, 15), 31));

        var response = service.listUsers(1, 15, " lan ", "teacher", "premium", true);

        assertThat(response.getItems()).extracting("email").containsExactly("lan@example.com");
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(15);
        assertThat(response.getTotalItems()).isEqualTo(31);
        assertThat(response.getTotalPages()).isEqualTo(3);
        verify(profiles).findAdminUsers(
                eq("lan"), eq("TEACHER"), eq("PREMIUM"), eq(true),
                eq(PageRequest.of(1, 15, org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))));
    }

    @Test
    void createsUserAndReturnsTempPasswordWhenMailDisabled() {
        UUID actor = UUID.randomUUID();
        when(currentUser.getUserId()).thenReturn(Optional.of(actor));
        when(profiles.findByEmail("new@histar.vn")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(mailProperties.isEnabled()).thenReturn(false);
        when(appProperties.getFrontendUrl()).thenReturn("http://localhost:5173");
        when(profiles.save(any(Profile.class))).thenAnswer(invocation -> {
            Profile p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        var created = service.createUser(
                new CreateAdminUserRequest("new@histar.vn", "New User", "USER", true));

        assertThat(created.accountCreated()).isTrue();
        assertThat(created.credentialsEmailed()).isFalse();
        assertThat(created.temporaryPassword()).isNotBlank();
        assertThat(created.role()).isEqualTo("USER");
        verify(audit).append(any());
    }

    @Test
    void createUserRejectsDuplicateEmail() {
        when(currentUser.getUserId()).thenReturn(Optional.of(UUID.randomUUID()));
        when(profiles.findByEmail("dup@histar.vn"))
                .thenReturn(Optional.of(profile(UUID.randomUUID(), "USER")));

        assertThatThrownBy(() -> service.createUser(
                        new CreateAdminUserRequest("dup@histar.vn", null, "USER", false)))
                .isInstanceOf(ConflictException.class);
        verify(profiles, never()).save(any());
    }

    @Test
    void blocksSelfChangingFromAdminToAnyNonAdminRole() {
        UUID id = UUID.randomUUID();
        when(currentUser.getUserId()).thenReturn(Optional.of(id));
        when(profiles.findByIdForUpdate(id)).thenReturn(Optional.of(profile(id, "ADMIN")));

        assertThatThrownBy(() -> service.updateRole(id,
                new UpdateUserRoleRequest("TEACHER", "Điều chỉnh phân quyền theo nhiệm vụ", "ADMIN")))
                .isInstanceOf(BusinessRuleException.class);
        verify(profiles, never()).save(any());
        verify(audit, never()).append(any());
    }

    @Test
    void blocksDemotingLastAdminUnderPessimisticLock() {
        UUID actor = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        Profile targetProfile = profile(target, "ADMIN");
        when(currentUser.getUserId()).thenReturn(Optional.of(actor));
        when(profiles.findByIdForUpdate(target)).thenReturn(Optional.of(targetProfile));
        when(profiles.findAllAdminsForUpdate()).thenReturn(List.of(targetProfile));

        assertThatThrownBy(() -> service.updateRole(target,
                new UpdateUserRoleRequest("USER", "Tài khoản không còn nhiệm vụ quản trị", "ADMIN")))
                .isInstanceOf(BusinessRuleException.class);
        verify(audit, never()).append(any());
    }

    @Test
    void rejectsStaleExpectedRole() {
        UUID actor = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        when(currentUser.getUserId()).thenReturn(Optional.of(actor));
        when(profiles.findByIdForUpdate(target)).thenReturn(Optional.of(profile(target, "TEACHER")));

        assertThatThrownBy(() -> service.updateRole(target,
                new UpdateUserRoleRequest("USER", "Điều chỉnh vai trò theo hồ sơ mới", "ADMIN")))
                .isInstanceOf(ConflictException.class);
        verify(audit, never()).append(any());
    }

    @Test
    void changesRoleAndAuditsExactlyOnce() {
        UUID actor = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        Profile profile = profile(target, "USER");
        when(currentUser.getUserId()).thenReturn(Optional.of(actor));
        when(profiles.findByIdForUpdate(target)).thenReturn(Optional.of(profile));
        when(profiles.save(profile)).thenReturn(profile);

        service.updateRole(target,
                new UpdateUserRoleRequest("TEACHER", "Assigned to classroom review duties", "USER"));

        verify(audit, times(1)).append(any());
    }

    private Profile profile(UUID id, String role) {
        return Profile.builder().id(id).email(id + "@example.com").role(role).build();
    }
}
