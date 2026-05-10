# FarmConnect: Enterprise Agricultural Supply Chain Ecosystem

![FarmConnect Hero Image](assets/hero.png)

[![Backend CI/CD](https://github.com/SamppurnaTH/FarmConnect/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/SamppurnaTH/FarmConnect/actions)
[![Frontend CI/CD](https://github.com/SamppurnaTH/FarmConnect/actions/workflows/frontend-ci.yml/badge.svg)](https://github.com/SamppurnaTH/FarmConnect/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Version](https://img.shields.io/badge/Version-1.2.0-green.svg)]()

**FarmConnect** is a decentralized, resilient, and enterprise-grade agritech platform designed to digitize the entire agricultural value chain. By bridging the gap between farmers, traders, and government programs, it provides a unified source of truth for market transactions, subsidy management, and regulatory compliance.

---

## 👨‍💻 Developed By
**SamppurnaTH** - *Lead Architect & Developer*

---

## 🚀 DevOps & Workflow Status
We maintain high standards for code quality and reliability. Our development workflow is fully automated:

- **Pipeline Status**: [Check GitHub Actions](https://github.com/SamppurnaTH/FarmConnect/actions)
- **CI/CD Workflow**: [Learn more about our Git Workflow](docs/git-workflow.md)
- **Documentation**: [Browse detailed Service Docs](docs/services/)

---

## ✨ Key Value Propositions

- **🔗 Transparency**: End-to-end traceability of agricultural produce from farm to market.
- **🌾 Efficiency**: Automated subsidy allocation and eligibility verification using institutional data.
- **⚡ Scalability**: Cloud-native microservices architecture capable of handling high-concurrency transactions.
- **🔐 Security**: Robust identity management (RBAC), JWT authentication, and audit trails.
- **🛡️ Resilience**: Built-in fault tolerance with **Resilience4j** Circuit Breakers and Retries.

---

## 🏗️ Technical Architecture

FarmConnect is built on a **High-Concurrency Microservices Architecture**. Click on a service to view its **detailed technical documentation**:

| Domain | Service | Responsibilities | Documentation |
| :--- | :--- | :--- | :--- |
| **Edge Gateway** | `gateway-service` | Central Entry Point, Dynamic Routing | [View Doc](docs/services/core-services.md#gateway-service) |
| **Discovery** | `eureka-service` | Service Registration, Monitoring | [View Doc](docs/services/core-services.md#eureka-service) |
| **Identity** | `identity-service` | Security, RBAC, JWT, Audit Logging | [Read Deep Dive](docs/services/identity-service.md) |
| **Farmer** | `farmer-service` | Profile Lifecycle, KYC, Verification | [Read Deep Dive](docs/services/farmer-service.md) |
| **Marketplace** | `crop-service` | Inventory, Listing Management | [View Summary](docs/services/core-services.md) |
| **Commerce** | `transaction-service` | Trade Execution, Settlement | [View Summary](docs/services/core-services.md) |
| **Governance** | `subsidy-service` | Grant Allocation, Disbursement | [View Summary](docs/services/core-services.md) |
| **Assurance** | `compliance-service` | Regulatory Checkpoints | [View Summary](docs/services/core-services.md) |
| **Intelligence** | `reporting-service` | KPI Dashboards, Analytics | [View Summary](docs/services/core-services.md) |

---

## 🛠️ Technology Stack

| Component | Technologies |
| :--- | :--- |
| **Backend** | Java 17, Spring Boot 3.2.5, Spring Cloud Netflix, Resilience4j |
| **Database** | PostgreSQL 15, Redis 7 (Caching & Tokens), Flyway |
| **Frontend** | React 18, TypeScript, Vite, Tailwind CSS (Modern Theme) |
| **API Docs** | Swagger / OpenAPI 3.0 (SpringDoc) |
| **Observability** | Structured JSON Logging (Logstash), Micrometer |
| **Deployment** | Docker Engine, Docker Compose, GitHub Actions |

---

## 🏁 Operational Quick-Start

### 1. Environment Setup
```bash
cp .env.example .env
# Open .env and set your JWT_SECRET and ENCRYPTION_KEY
```

### 2. Launch with Docker
```bash
docker-compose up -d --build
```

### 3. Access Points
- **Web Portal**: [http://localhost:80](http://localhost:80)
- **API Gateway**: [http://localhost:8080](http://localhost:8080)
- **Swagger UI**: `http://localhost:8080/{service-name}/swagger-ui/index.html`

---

## 🔑 Demo Accounts

| Role | Username | Password |
| :--- | :--- | :--- |
| **Administrator** | `admin_demo` | `Admin@1234` |
| **Farmer** | `farmer_demo` | `Farm@1234` |
| **Trader** | `trader_demo` | `Trade@1234` |
| **Market Officer** | `officer_demo` | `Officer@1234` |

---
© 2026 FarmConnect Platform - Bridging the Digital Divide in Agriculture.
Developed with ❤️ by **SamppurnaTH**.
