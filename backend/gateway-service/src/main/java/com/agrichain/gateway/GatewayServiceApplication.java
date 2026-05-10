package com.agrichain.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Spring Cloud Gateway — the single entry point for all external traffic.
 *
 * Replaces the Vite dev proxy and hardcoded service URLs with a production-grade
 * API gateway that:
 * - Discovers services via Eureka
 * - Routes requests based on service IDs
 * - Provides a single point for cross-cutting concerns (auth, rate limiting, CORS)
 *
 * Route configuration is in GatewayRouteConfig.
 * Correlation ID & CORS handling is in RequestProcessingFilter and CorsConfig.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}