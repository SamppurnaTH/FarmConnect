# KrishiSetu
**Bridging the Gap between Farmers, Traders, and Government Programs**

## Overview
KrishiSetu is a comprehensive agricultural supply chain management platform designed to digitize and streamline the ecosystem of farming, trading, and government interventions. It solves the real-world problem of fragmented communication and lack of transparency in the agricultural sector by providing a unified digital interface for all stakeholders—ensuring that farmers get fair access to markets and subsidies while providing government bodies with robust compliance and reporting tools.

## Key Features
*   **Decentralized Identity Management**: Secure authentication and role-based access control (RBAC) specifically tailored for Farmers, Traders, Market Officers, and Admins.
*   **Digital Farmer Onboarding**: Automated registration workflow with support for document uploads, land detail verification, and profile management.
*   **Crop & Inventory Tracking**: Direct-from-farm crop listing and management, allowing for better visibility into available agricultural produce.
*   **Integrated Subsidy Pipeline**: End-to-end management of government subsidy programs, including application processing and disbursement tracking.
*   **Market Compliance Engine**: Automated verification of documents and transactions to ensure adherence to agricultural standards and regulations.
*   **Multi-Dimensional Reporting**: Specialized dashboards for tracking market trends, transaction volumes, and compliance metrics.
*   **Real-time Notifications**: Alert system for status updates on registrations, payments, and program eligibility.

## System Architecture
The system follows a **Microservices Architecture** to ensure scalability and independent deployment of specialized business domains.
*   **Frontend**: A modern Single Page Application (SPA) built with React, utilizing a modular component architecture and centralized state management.
*   **Backend**: 9 independent Spring Boot microservices, each responsible for a specific domain (Identity, Farmer, Crop, Transaction, etc.).
*   **Persistence**: A "Database per Service" pattern using PostgreSQL to ensure strict data isolation and domain integrity.
*   **API Gateway/Proxy**: Nginx is used in production (and Vite proxy in development) to route traffic from the frontend to the appropriate microservice ports (8081-8089).

## Tech Stack
*   **Frontend**: React 18, TypeScript, Vite, Tailwind CSS, Lucide React (Icons), Axios (API Client).
*   **Backend**: Java 17, Spring Boot 3.2.5, Spring Data JPA, Hibernate, Maven.
*   **Database**: PostgreSQL 15.
*   **DevOps**: Docker, Nginx.

## Installation & Setup

### Prerequisites
*   Java 17 or higher
*   Node.js 18.x or higher
*   Docker & Docker Compose
*   Maven 3.8+

### Step-by-Step Setup
1.  **Clone the Repository**:
    ```bash
    git clone <repository-url>
    cd Agri
    ```

2.  **Build the Backend**:
    ```bash
    cd agri-chain
    mvn clean package -DskipTests
    cd ..
    ```

3.  **Run with Docker Compose**:
    ```bash
    docker-compose up -d --build
    ```

4.  **Access the Application**:
    - Frontend: `http://localhost:80`
    - Backend Services: `http://localhost:8081` through `8089`

## Usage

### Farmer Registration Flow
1.  Navigate to the **Register** page.
2.  Fill in personal details (Name, Contact, Address) and land specifications.
3.  Upload required identification and land ownership documents.
4.  Once submitted, the account enters `Pending_Verification` status, awaiting review by a Market Officer.

### Crop Listing Flow
1.  Log in as a **Farmer**.
2.  Navigate to **My Crops** section.
3.  Create a new listing with crop type, quantity, and expected pricing.
4.  The listing becomes visible to registered **Traders** for potential procurement.

## API Documentation

### Important Endpoints

| Service | Endpoint | Method | Description |
| :--- | :--- | :--- | :--- |
| **Identity** | `/audit-log` | `GET` | Retrieve system-wide audit records |
| **Farmer** | `/farmers` | `POST` | Register a new farmer profile |
| **Crop** | `/crops` | `GET` | List all available agricultural produce |
| **Subsidy** | `/programs` | `POST` | Create a new government subsidy program |
| **Trader** | `/traders/{id}` | `GET` | Retrieve trader performance and profile |

**Sample Registration Request (`POST /api/farmers`):**
```json
{
  "name": "Venu Thota",
  "contactInfo": "+919505511839",
  "address": "Sri Ram Nagar",
  "landDetails": "2.5 Acres - Paddy",
  "gender": "Male"
}
```

## Folder Structure
```text
Agri/
├── agri-chain/               # Backend Microservices (Java/Spring)
│   ├── identity-service/     # Auth and RBAC
│   ├── farmer-service/       # Farmer domain logic
│   ├── crop-service/         # Crop marketplace logic
│   ├── common/               # Shared DTOs and Enums
│   └── ...                   # Other domain services (subsidy, compliance, etc.)
├── agri-chain-frontend/      # Frontend Application (React/Vite)
│   ├── src/api/              # Axios API clients
│   ├── src/pages/            # Role-based UI components
│   └── src/stores/           # Zustand state management
├── docker-compose.yml        # Multi-container orchestration
└── init.sql                  # Database initialization scripts
```

## Future Enhancements
*   **Blockchain Integration**: Implement a Distributed Ledger (Hyperledger Fabric/Ethereum) for immutable record-keeping of transactions.
*   **AI/ML Price Prediction**: Integrate predictive models to suggest optimal selling prices based on market trends.
*   **Offline Support**: Progressive Web App (PWA) features for farmers in low-connectivity areas.
*   **IoT Integration**: Support for soil moisture sensors and weather stations via a dedicated IoT microservice.

## Contribution Guidelines
1.  Fork the project.
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`).
3.  Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4.  Push to the branch (`git push origin feature/AmazingFeature`).
5.  Open a Pull Request.

## License
Suggested: **MIT License**.
