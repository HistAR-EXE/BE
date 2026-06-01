package com.histar.be.security;

import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final ProfileService profileService;

    @Override
    public UserDetails loadUserByUsername(String email) {
        Profile profile = profileService
                .findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Not found: " + email));
        return User.builder()
                .username(profile.getEmail())
                .password(profile.getPasswordHash() == null ? "" : profile.getPasswordHash())
                .authorities("ROLE_" + (profile.getRole() == null ? "USER" : profile.getRole()))
                .build();
    }
}
