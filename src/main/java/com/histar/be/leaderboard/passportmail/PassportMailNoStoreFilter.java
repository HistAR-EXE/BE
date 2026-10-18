package com.histar.be.leaderboard.passportmail;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Applies also to authentication and multipart-parser failures, before Spring Security runs. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PassportMailNoStoreFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        String path=request.getRequestURI().substring(request.getContextPath().length());
        if(path.equals("/api/leaderboard/passport/email") || path.startsWith("/api/leaderboard/passport/email/"))response.setHeader("Cache-Control","private, no-store");
        chain.doFilter(request,response);
    }
}
