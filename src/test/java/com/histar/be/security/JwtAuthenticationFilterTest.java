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

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, users,
                mock(com.histar.be.billing.service.CcuSessionService.class), new com.fasterxml.jackson.databind.ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer deleted-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test void directApiWithoutHeartbeatIsRejectedWhenOrganizationCcuIsFull() throws Exception {
        var jwt = mock(JwtService.class);
        var users = mock(CustomUserDetailsService.class);
        var ccu = mock(com.histar.be.billing.service.CcuSessionService.class);
        when(jwt.isValid("token")).thenReturn(true);
        when(jwt.extractEmail("token")).thenReturn("student@histar.local");
        when(users.loadUserByUsername("student@histar.local")).thenReturn(
                org.springframework.security.core.userdetails.User.withUsername("student@histar.local")
                        .password("hash").roles("ORG_MEMBER").build());
        org.mockito.Mockito.doThrow(new com.histar.be.common.exception.CcuLimitException("Đã đầy CCU",15,15))
                .when(ccu).admitRequest("student@histar.local");
        var filter = new JwtAuthenticationFilter(jwt, users, ccu, new com.fasterxml.jackson.databind.ObjectMapper());
        var request = new MockHttpServletRequest("GET", "/api/quests");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("CCU_LIMIT_EXCEEDED");
        org.mockito.Mockito.verifyNoInteractions(chain);
    }

    @Test void ownProfileOverviewRemainsReadableWithoutConsumingAnOrganizationSeat() throws Exception {
        var jwt = mock(JwtService.class);
        var users = mock(CustomUserDetailsService.class);
        var ccu = mock(com.histar.be.billing.service.CcuSessionService.class);
        when(jwt.isValid("token")).thenReturn(true);
        when(jwt.extractEmail("token")).thenReturn("student@histar.local");
        when(users.loadUserByUsername("student@histar.local")).thenReturn(
                org.springframework.security.core.userdetails.User.withUsername("student@histar.local")
                        .password("hash").roles("ORG_MEMBER").build());
        var filter = new JwtAuthenticationFilter(jwt, users, ccu, new com.fasterxml.jackson.databind.ObjectMapper());
        var request = new MockHttpServletRequest("GET", "/api/profile/overview");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        org.mockito.Mockito.verifyNoInteractions(ccu);
        verify(chain).doFilter(request, response);
    }
}
