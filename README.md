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

## 🚀 Project Overview & Strategy
We maintain a professional and transparent development process. Explore our strategic and operational documents:

- **[Product Requirements Document (PRD)](docs/PRD.md)**: Vision, features, and roadmap.
- **[Git Workflow & CI/CD](docs/git-workflow.md)**: Automated pipelines and quality standards.
- **[Architecture Deep Dives](docs/services/)**: Technical specifications for individual services.
- **[Pipeline Status](https://github.com/SamppurnaTH/FarmConnect/actions)**: Real-time build and test results.

---

## ✨ Key Value Propositions

- **🔗 Transparency**: End-to-end traceability of agricultural produce from farm to market.
- **🌾 Efficiency**: Automated subsidy allocation and eligibility verification using institutional data.
- **⚡ Scalability**: Cloud-native microservices architecture capable of handling high-concurrency transactions.
- **🔐 Security**: Robust identity management (RBAC), JWT authentication, and audit trails.
- **🛡️ Resilience**: Built-in fault tolerance with **Resilience4j** Circuit Breakers and Retries.

---

## 🏗️ Technical Architecture

FarmConnect is built on a **High-Concurrency Microservices Architecture**. In production, only the gateway-service and frontend are exposed externally. All internal microservices communicate through the gateway via service discovery.

Click on a service to view its **detailed technical documentation**:

| Domain | Service | Responsibilities | Documentation |
| :--- | :--- | :--- | :--- |
| **Edge Gateway** | `gateway-service` | Central Entry Point, Dynamic Routing, Rate Limiting | [View Doc](docs/services/core-services.md#gateway-service) |
| **Discovery** | `eureka-service` | Service Registration, Health Monitoring, Load Balancing | [View Doc](docs/services/core-services.md#eureka-service) |
| **Identity** | `identity-service` | Security, RBAC, JWT, Audit Logging, Session Management | [Read Deep Dive](docs/services/identity-service.md) |
| **Farmer** | `farmer-service` | Profile Lifecycle, KYC, Verification, Document Management | [Read Deep Dive](docs/services/farmer-service.md) |
| **Marketplace** | `crop-service` | Inventory, Listing Management, Price Indexing | [View Summary](docs/services/core-services.md) |
| **Commerce** | `transaction-service` | Trade Execution, Settlement, Payment Processing | [View Summary](docs/services/core-services.md) |
| **Governance** | `subsidy-service` | Grant Allocation, Disbursement, Eligibility Verification | [View Summary](docs/services/core-services.md) |
| **Assurance** | `compliance-service` | Regulatory Checkpoints, Audit Trails, Compliance Reporting | [View Summary](docs/services/core-services.md) |
| **Intelligence** | `reporting-service` | KPI Dashboards, Market Analytics, Business Intelligence | [View Summary](docs/services/core-services.md) |
| **Notification** | `notification-service` | Multi-channel Alerts (In-App, Email, SMS), Template Management | [View Summary](docs/services/core-services.md) |
| **Trader** | `trader-service` | Trader Profile, Procurement Tracking, Bid Management | [View Summary](docs/services/core-services.md) |

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

---


---

## 📄 License
This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.

---
© 2026 FarmConnect Platform - Bridging the Digital Divide in Agriculture.
Developed with ❤️ by **SamppurnaTH**.
