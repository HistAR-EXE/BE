package com.histar.be.security;

import jakarta.servlet.DispatcherType;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final TestHookSecurityFilter testHookSecurityFilter;
    private final CustomUserDetailsService userDetailsService;
    private final SecurityExceptionHandlers securityExceptionHandlers;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // SSE (live board): async re-dispatch skips the JWT filter (OncePerRequestFilter); the
                        // initial request was already authenticated + authorized in the controller.
                        .dispatcherTypeMatchers(DispatcherType.ASYNC)
                        .permitAll()
                        .requestMatchers("/api/org/*/live-board/**")
                        .authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/events/batch", "/api/referral/*/visit")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/referral/*", "/api/referral/*/stats")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/locations/*/secret-story")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/billing/org/plans", "/api/billing/public-pricing")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/pilot-sites", "/api/pilot-sites/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/panoramas/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/sites/*/stations", "/api/sites/*/stations/*", "/api/sites/*/story", "/api/sites/*/pack", "/api/sites/*/pack/faq_offline.json", "/api/sites/*/stations/*/chat-prompts", "/api/sites/*/stations/*/games", "/api/sites/*/media-manifest")
                        .permitAll()
                        .requestMatchers("/ws/squad/**")
                        .permitAll()
                        .requestMatchers(
                                "/api/health/**",
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/google",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/api/auth/verify-email/confirm",
                                "/api/billing/webhooks/sepay",
                                "/api/test/**",
                                "/api/locations/**",
                                "/api/characters/**",
                                "/api/photo-pairs/**",
                                "/api/photo-scenes/**",
                                "/api/artifacts",
                                "/api/discovery-points/**",
                                "/api/hotspots/**",
                                "/api/quests",
                                "/api/badges",
                                "/api/photo-frames",
                                "/api/leaderboard",
                                "/api/share/prefill")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(securityExceptionHandlers)
                        .accessDeniedHandler(securityExceptionHandlers))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(testHookSecurityFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
