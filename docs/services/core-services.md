# Gateway & Discovery

The **Infrastructure Layer** provides the backbone for service communication, routing, and visibility.

## 🌉 Gateway Service (`gateway-service`)
The entry point for all client traffic (Frontend & External APIs).
- **Routing**: Dynamic routing based on Eureka service registry.
- **Security**: Centralized CORS configuration.
- **Port**: `8080`

## 🔎 Eureka Service (`eureka-service`)
The service discovery server.
- **Registration**: All microservices register their location and health status.
- **Heartbeats**: Monitors service availability.
- **Dashboard**: Web UI at `http://localhost:8761`.
- **Port**: `8761`

---

# Core Microservices (Summary)

| Service | Port | Primary Responsibility |
| :--- | :--- | :--- |
| `crop-service` | `8083` | Inventory, Price Indexing, Listing Management |
| `transaction-service` | `8084` | Secure Trade Execution, Settlement |
| `subsidy-service` | `8085` | Grant Allocation & Disbursement |
| `compliance-service` | `8086` | Automated Regulatory Checkpoints |
| `reporting-service` | `8087` | KPI Dashboards & Market Analytics |
| `notification-service` | `8088` | Multi-channel Alerts (In-App, Email) |
| `trader-service` | `8089` | Trader Profile & Procurement Tracking |
