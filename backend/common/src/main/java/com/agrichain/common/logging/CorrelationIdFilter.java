package com.agrichain.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that generates or extracts a correlation ID for every request.
 *
 * <p>If the request already contains {@code X-Request-Id}, we use it (maintains
 * trace continuity from the gateway). Otherwise we generate a new UUID.</p>
 *
 * <p>This filter runs in every microservice so that logs captured at the service
 * level carry the same correlation ID passed through the gateway.</p>
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);
    public static final String CORRELATION_ID_HEADER = "X-Request-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Add to MDC so every log line in this request includes the correlation ID
        org.slf4j.MDC.put(MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        log.debug("Request {} {} | correlationId={}", request.getMethod(), request.getRequestURI(), correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            org.slf4j.MDC.remove(MDC_KEY);
        }
    }
}