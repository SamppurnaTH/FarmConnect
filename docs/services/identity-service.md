# Identity Service

The **Identity Service** is the central security and authorization hub for the FarmConnect ecosystem. It handles user authentication, Role-Based Access Control (RBAC), and maintains immutable audit trails for security-sensitive operations.

## 🔐 Core Responsibilities
- **Authentication**: JWT-based login, refresh, and logout.
- **Authorization**: Fine-grained RBAC for Farmers, Traders, and Officers.
- **Token Management**: Secure storage and invalidation of JWT IDs (JTIs) in Redis.
- **Audit Logging**: Recording system-wide actions for compliance and accountability.

## 📡 API Endpoints

### Authentication
| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/login` | Validate credentials and issue JWT. | No |
| `POST` | `/auth/register` | Create a new system user. | No |
| `POST` | `/auth/logout` | Invalidate current JWT. | Yes (Bearer) |
| `POST` | `/auth/refresh` | Issue new JWT for active session. | Yes (Bearer) |

### Role Management
| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `PUT` | `/roles/{userId}` | Update a user's role. | Yes (Admin) |

### Audit
| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/audit-log` | Record a new audit entry. | Internal |
| `GET` | `/audit-log` | Retrieve audit entries (paginated). | Yes (Admin) |

## 🛠️ Configuration
- **Database**: PostgreSQL (`agrichain_identity`)
- **Cache**: Redis (Token Store)
- **Security**: Spring Security + JJWT
- **Port**: `8081`

## 🔗 Dependencies
- `eureka-service`: Service Registration
- `redis`: Token Invalidation
