# Identity Service: Security & Governance Core

The **Identity Service** is the high-security foundation of the FarmConnect ecosystem. It implements a stateless authentication mechanism using JWTs and manages a strict Role-Based Access Control (RBAC) model.

## 🔐 Security Architecture

### Stateless Authentication
Identity Service uses **JSON Web Tokens (JWT)** for session management. When a user logs in, a unique JTI (JWT ID) is generated and stored in **Redis** with a TTL matching the token's expiry.
- **Logout/Invalidation**: To logout, the JTI is removed from Redis. Any token with a missing JTI is rejected, even if its signature is valid.
- **Refresh Flow**: To maintain security, tokens are short-lived (e.g., 1 hour). The refresh endpoint allows issuing a new token without re-entering credentials, provided the old token is still valid.

### RBAC Model (Roles)
The system supports the following hierarchical roles:
1. **ADMINISTRATOR**: Full system access, role management, and audit inspection.
2. **MARKET_OFFICER**: Government/Agency representative. Can verify farmers, approve subsidies, and view regional reports.
3. **TRADER**: Commercial entity. Can place bids, execute transactions, and manage procurement profiles.
4. **FARMER**: Primary producer. Can manage land details, upload KYC docs, and apply for subsidies.

## 📡 Detailed API Specification

### 1. Authentication Interface
| Endpoint | Method | Payload | Description |
| :--- | :--- | :--- | :--- |
| `/auth/login` | `POST` | `{"username", "password"}` | Validates credentials against BCrypt hashes. Returns `token`, `role`, and `userId`. |
| `/auth/register` | `POST` | `{"username", "password", "email", "role"}` | Creates a new user. Default role is `FARMER` if unspecified. |
| `/auth/refresh` | `POST` | `Bearer Token` | Validates current token and issues a new one with a fresh JTI. |
| `/auth/logout` | `POST` | `Bearer Token` | Blacklists the current JTI in Redis immediately. |

### 2. Role Governance
- **`PUT /roles/{userId}/assignment`**: Allows an Administrator to promote or demote users. This triggers a session invalidation for the target user to ensure role changes take effect on the next login.

### 3. Immutable Audit Trails
The service provides a centralized audit sink for all microservices.
- **`POST /audit-log`**: Internal endpoint for services to report sensitive actions.
- **Data Captured**: `timestamp`, `serviceName`, `userId`, `action`, `resourceId`, `status` (Success/Failure), and `ipAddress`.

## 🛠️ Internal Implementation Details

### Data Persistence
- **PostgreSQL**: Stores user credentials, profiles, and historical audit logs.
- **Redis**: High-speed store for active JTIs and rate-limiting counters.

### Security Filters
The service uses a `JwtAuthenticationFilter` that intercepts every request to verify:
1. Signature validity (HMAC SHA-256).
2. Expiry status.
3. JTI presence in Redis.

## 🔗 System Integration
- **Gateway Service**: Routes all `/api/auth/**` traffic here.
- **Farmer Service**: Calls `/auth/register` during the farmer onboarding workflow.
- **Reporting Service**: Queries `/audit-log` to generate security compliance reports.
