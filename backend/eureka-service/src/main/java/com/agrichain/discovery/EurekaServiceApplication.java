package com.agrichain.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Service Discovery Server.
 *
 * All microservices register themselves with Eureka on startup.
 * The Spring Cloud Gateway uses Eureka to discover available service instances.
 *
 * Transitionally replaces hardcoded service URLs (e.g. http://farmer-service:8082)
 * with dynamic lookup. Later, when Kubernetes DNS is ready, services can switch to
 * K8s service discovery with minimal code change (just config).
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServiceApplication.class, args);
    }
}