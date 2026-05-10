package com.agrichain.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Global gateway filter that:
 * 1. Generates and propagates a correlation ID (X-Request-Id) for tracing
 * 2. Passes the Authorization header through to downstream services
 * 3. Extracts userId from JWT (via gateway-level JWT parsing) for internal routing
 *
 * This ensures all downstream services receive the correlation ID for
 * distributed tracing without each service needing to generate its own.
 */
@Component
public class RequestProcessingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestProcessingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. Generate or extract correlation ID
        String rawRequestId = exchange.getRequest().getHeaders().getFirst("X-Request-Id");
        if (rawRequestId == null || rawRequestId.isBlank()) {
            rawRequestId = UUID.randomUUID().toString();
        }
        final String requestId = rawRequestId;

        // 2. Add correlation ID to downstream request
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-Request-Id", requestId)
                .build();

        // 3. Log request with correlation ID for debugging
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod() != null ? exchange.getRequest().getMethod().name() : "UNKNOWN";
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        boolean hasToken = authHeader != null && authHeader.startsWith("Bearer ");
        log.info("[gateway] {} {} | requestId={} | auth={}", method, path, requestId, hasToken);

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        return chain.filter(mutatedExchange).then(Mono.fromRunnable(() -> {
            // Log response status
            int statusCode = mutatedExchange.getResponse().getStatusCode() != null
                    ? mutatedExchange.getResponse().getStatusCode().value()
                    : 0;
            log.info("[gateway] {} {} → {} | requestId={}", method, path, statusCode, requestId);
        }));
    }

    @Override
    public int getOrder() {
        return -1; // Run early - before routing
    }
}