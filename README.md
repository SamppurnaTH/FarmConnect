# FarmConnect: Enterprise Agricultural Supply Chain Ecosystem

[![Backend CI/CD](https://github.com/SamppurnaTH/KrishiSetu/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/SamppurnaTH/KrishiSetu/actions)
[![Frontend CI/CD](https://github.com/SamppurnaTH/KrishiSetu/actions/workflows/frontend-ci.yml/badge.svg)](https://github.com/SamppurnaTH/KrishiSetu/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**FarmConnect** is a decentralized, resilient, and enterprise-grade agritech platform designed to digitize the entire agricultural value chain. By bridging the gap between farmers, traders, and government programs, it provides a unified source of truth for market transactions, subsidy management, and regulatory compliance.

---

## 🏗️ Technical Architecture

FarmConnect is built on a **High-Concurrency Microservices Architecture**, ensuring domain isolation, horizontal scalability, and fault tolerance.

### Service Mesh & Orchestration
The ecosystem comprises 9 specialized Spring Boot microservices, coordinated via a robust container orchestration layer:

| Domain | Service | Responsibilities |
| :--- | :--- | :--- |
| **Identity** | `identity-service` | RBAC, JWT Issuance, OAuth2, System-wide Audit Logging |
| **Farmer** | `farmer-service` | Profile Lifecycle, KYC Documents, Land Verification |
| **Marketplace** | `crop-service` | Decentralized Inventory, Price Indexing, Listing Management |
| **Commerce** | `transaction-service` | Secure Trade Execution, Settlement, Digital Ledger Entry |
| **Governance** | `subsidy-service` | Grant Allocation, Eligibility Scoring, Program Disbursement |
| **Assurance** | `compliance-service` | Automated Verification, Regulatory Checkpoints, Fraud Detection |
| **Intelligence** | `reporting-service` | KPI Dashboards, Market Analytics, Periodic PDF/JSON Reports |
| **Engagement** | `notification-service` | Multi-channel Alerts, Event-driven Status Updates |
| **Operations** | `trader-service` | Trader Reputation, Performance Metrics, Procurement Flow |

### Data Strategy
- **Persistence Layer**: Each service encapsulates its own **PostgreSQL 15** database, adhering strictly to the "Database per Service" pattern.
- **Resilience**: Integrated Docker Health Checks and robust `depends_on` conditions ensure a deterministic system startup.

---

## 🛠️ Technology Stack

| Component | technologies |
| :--- | :--- |
| **Core Framework** | Java 17, Spring Boot 3.2.5, Spring Data JPA |
| **Frontend Ecosystem** | React 18, TypeScript, Vite, Tailwind CSS, Lucide |
| **Security Architecture** | JWT (JSON Web Tokens), BCrypt, Spring Security |
| **Infrastucture** | Docker Engine, Docker Compose, Nginx Proxy |
| **DevOps & CI/CD** | GitHub Actions, GHCR (GitHub Container Registry) |

---

## 🚀 Operational Quick-Start

### Prerequisites
- **Runtimes**: Java 17+, Node.js 20+
- **Tooling**: Maven 3.8+, Docker Desktop / Engine
- **Resources**: Minimum 8GB RAM recommended for full-stack orchestration

### Deployment Workflow
1. **Cloud-Native Packaging**:
   Assemble the microservice binaries using Maven:
   ```bash
   cd backend
   mvn clean package -DskipTests
   cd ..
   ```

2. **Containerized Orchestration**:
   Launch the entire FarmConnect ecosystem with a single command:
   ```bash
   docker-compose up -d --build
   ```

3. **Infrastructural Verification**:
   Ensure all 11 core components are operational:
   - **Frontend Interface**: `http://localhost:80`
   - **Backend API Mesh**: `ports 8081-8089` (Routed through internal Nginx proxy)

---

## 🔐 Security & Compliance

FarmConnect prioritizes institutional data integrity and security through multiple layers of fortification:
- **JWT-Based Authentication**: Secure, stateless session management across the microservices mesh.
- **Granular RBAC**: Role-Based Access Control specifically optimized for **Farmers**, **Traders**, **Market Officers**, and **Administrators**.
- **Comprehensive Audit Trail**: Every transaction and status change is immutably logged with correlation IDs for forensic reporting.
- **Encrypted Data Flows**: Support for industry-standard encryption for sensitive PII (Personally Identifiable Information).

---

## 📈 Strategic Roadmap

- [ ] **Blockchain Ledger Integration**: Implementing Hyperledger Fabric for immutable trade tracking.
- [ ] **AI-Driven Analytics**: Predictive algorithms for regional crop yield and market price suggestions.
- [ ] **IoT Sensor Mesh**: Support for real-time soil and weather telemetry via specialized edge-connectors.
- [ ] **Internationalization (i18n)**: Multi-language support to cater to regional agricultural hubs.

---

## 🤝 Contribution & Governance

Institutional contributions are welcome. Please adhere to the established workflow:
1. Feature Branching (`feature/enhanced-telemetry`)
2. Structured Commits (Conventional Commits)
3. Mandatory PR Review for Merges into `main`

---

## 📄 License
This project is licensed under the **MIT Enterprise License**.

---
© 2026 FarmConnect Platform - Bridging the Digital Divide in Agriculture.
