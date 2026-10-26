package com.histar.be.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deletedAccountTokenDoesNotEscapeAsServerError() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        CustomUserDetailsService users = mock(CustomUserDetailsService.class);
        when(jwtService.isValid("deleted-token")).thenReturn(true);
        when(jwtService.extractEmail("deleted-token")).thenReturn("deleted@demo.timelens.test");
        when(users.loadUserByUsername("deleted@demo.timelens.test"))
                .thenThrow(new UsernameNotFoundException("deleted"));

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, users);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer deleted-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }
}
