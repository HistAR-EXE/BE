package com.histar.be.admin.service;

import com.histar.be.admin.dto.AdminUserSummaryResponse;
import com.histar.be.admin.dto.UpdateUserRoleRequest;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.common.response.PageResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final ProfileRepository profileRepository;
    private final CurrentUserAccessor currentUserAccessor;

    public PageResponse<AdminUserSummaryResponse> listUsers(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<Profile> result = profileRepository.findAll(
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.<AdminUserSummaryResponse>builder()
                .items(result.getContent().stream().map(AdminUserSummaryResponse::from).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalItems(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional
    public AdminUserSummaryResponse updateRole(UUID userId, UpdateUserRoleRequest request) {
        Profile profile = profileRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        UserRole nextRole = UserRole.valueOf(request.role().trim().toUpperCase());
        UUID actorId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        if (actorId.equals(userId)
                && UserRole.fromStored(profile.getRole()) == UserRole.ADMIN
                && nextRole == UserRole.USER) {
            throw new BusinessRuleException("Không thể tự hạ quyền ADMIN của chính mình");
        }
        profile.setRole(nextRole.name());
        return AdminUserSummaryResponse.from(profileRepository.save(profile));
    }
}
