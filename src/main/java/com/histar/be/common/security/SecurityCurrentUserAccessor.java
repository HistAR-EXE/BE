package com.histar.be.common.security;

import com.histar.be.profile.service.ProfileService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityCurrentUserAccessor implements CurrentUserAccessor {

    private final ProfileService profileService;

    @Override
    public Optional<String> getEmail() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return Optional.of(userDetails.getUsername());
        }
        return Optional.empty();
    }

    @Override
    public Optional<UUID> getUserId() {
        return getEmail().flatMap(email -> profileService.findByEmail(email).map(profile -> profile.getId()));
    }
}
