package com.agrichain.farmer.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for farmer-service.
 *
 * - Stateless JWT-based auth (no sessions, no CSRF needed)
 * - POST /farmers is public (farmer self-registration)
 * - GET /farmers/{id}/status is internal (called by crop-service / subsidy-service)
 * - All other endpoints require a valid JWT
 * - Role-level enforcement is done via @PreAuthorize on controller methods
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public: farmer self-registration
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/farmers").permitAll()
                // Internal: status check called by other services (no user token in service-to-service calls)
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/farmers/*/status").permitAll()
                // Internal: farmer count called by reporting-service
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/farmers/count").permitAll()
                // Internal: farmer report called by reporting-service
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/farmers/report").permitAll()
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
