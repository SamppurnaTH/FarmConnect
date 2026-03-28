package com.agrichain.identity.security;

import com.agrichain.identity.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Filter that extracts and validates the JWT from the Authorization header.
 * 
 * If valid and present in the TokenStore, it populates the SecurityContext
 * with the user's details and role from the database.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenStore tokenStore;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, 
                                   TokenStore tokenStore, 
                                   UserRepository userRepository) {
        this.jwtService = jwtService;
        this.tokenStore = tokenStore;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        try {
            Claims claims = jwtService.parse(jwt);
            username = claims.getSubject();
            UUID tokenId = UUID.fromString(claims.getId());

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Check if token is still active in the store (not logged out)
                if (tokenStore.isActive(tokenId)) {
                    // Load user from DB to get the LATEST role (satisfies "within 1 minute" requirement)
                    userRepository.findByUsername(username).ifPresent(user -> {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                user.getUsername(),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    });
                }
            }
        } catch (JwtException | IllegalArgumentException e) {
            // Invalid token - just continue the chain; SecurityContext will remain empty
            // and subsequent filters (like anyRequest().authenticated()) will reject it.
        }

        filterChain.doFilter(request, response);
    }
}
