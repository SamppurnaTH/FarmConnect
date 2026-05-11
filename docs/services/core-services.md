# Gateway & Discovery

The **Infrastructure Layer** provides the backbone for service communication, routing, and visibility.

## 🌉 Gateway Service (`gateway-service`)
The entry point for all client traffic (Frontend & External APIs).
- **Routing**: Dynamic routing based on Eureka service registry.
- **Security**: Centralized CORS configuration.
- **Health Checks**: Available at `/actuator/health` (internal only)
- **Port**: `8080` (only service exposed externally in production)
- **Communication**: Routes to internal services via Eureka service discovery

## 🔎 Eureka Service (`eureka-service`)
The service discovery server.
- **Registration**: All microservices register their location and health status.
- **Heartbeats**: Monitors service availability.
- **Dashboard**: Web UI available internally (not exposed externally)
- **Port**: `8761` (internal only)

---

# Core Microservices (Summary)

All internal microservices communicate through the gateway and are not exposed externally:
- `crop-service`: Inventory, Price Indexing, Listing Management
- `transaction-service`: Secure Trade Execution, Settlement
- `subsidy-service`: Grant Allocation & Disbursement
- `compliance-service`: Automated Regulatory Checkpoints
- `reporting-service`: KPI Dashboards & Market Analytics
- `notification-service`: Multi-channel Alerts (In-App, Email)
- `trader-service`: Trader Profile & Procurement Tracking
- `farmer-service`: Profile Lifecycle, KYC, Verification
- `identity-service`: Security, RBAC, JWT, Audit Logging
