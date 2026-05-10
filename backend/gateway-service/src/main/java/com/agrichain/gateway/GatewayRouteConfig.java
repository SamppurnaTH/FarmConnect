package com.agrichain.gateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Centralized route configuration for Spring Cloud Gateway.
 *
 * Routes are mapped by service name — Spring Cloud Gateway resolves
 * service names via Eureka automatically when using lb:// URIs.
 *
 * Path-to-service mapping (mirrors the old Vite proxy config):
 * /api/auth/*          → identity-service      (port 8081)
 * /api/users/*         → identity-service
 * /api/farmers/*       → farmer-service        (port 8082)
 * /api/crops/*, /listings/*, /orders/* → crop-service (port 8083)
 * /api/transactions/*, /payments/* → transaction-service (port 8084)
 * /api/subsidies/*, /programs/*, /disbursements/* → subsidy-service (port 8085)
 * /api/compliance/*, /compliance-records/*, /audits/* → compliance-service (port 8086)
 * /api/reports/*, /dashboard/* → reporting-service (port 8087)
 * /api/notifications/* → notification-service   (port 8088)
 * /api/traders/*       → trader-service          (port 8089)
 */
@Configuration
public class GatewayRouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // ── Identity Service ────────────────────────────────────────────
                .route("identity-service", r -> r
                        .path("/api/auth/**", "/api/users/**", "/api/roles/**", "/api/audit-log/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://identity-service"))

                // ── Farmer Service ─────────────────────────────────────────────
                .route("farmer-service", r -> r
                        .path("/api/farmers/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://farmer-service"))

                // ── Crop Service ───────────────────────────────────────────────
                .route("crop-service", r -> r
                        .path("/api/crops/**", "/api/listings/**", "/api/orders/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://crop-service"))

                // ── Transaction Service ────────────────────────────────────────
                .route("transaction-service", r -> r
                        .path("/api/transactions/**", "/api/payments/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://transaction-service"))

                // ── Subsidy Service ────────────────────────────────────────────
                .route("subsidy-service", r -> r
                        .path("/api/subsidies/**", "/api/programs/**", "/api/disbursements/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://subsidy-service"))

                // ── Compliance Service ─────────────────────────────────────────
                .route("compliance-service", r -> r
                        .path("/api/compliance/**", "/api/compliance-records/**", "/api/audits/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://compliance-service"))

                // ── Reporting Service ──────────────────────────────────────────
                .route("reporting-service", r -> r
                        .path("/api/reports/**", "/api/dashboard/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://reporting-service"))

                // ── Notification Service ────────────────────────────────────────
                .route("notification-service", r -> r
                        .path("/api/notifications/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://notification-service"))

                // ── Trader Service ─────────────────────────────────────────────
                .route("trader-service", r -> r
                        .path("/api/traders/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://trader-service"))

                .build();
    }
}