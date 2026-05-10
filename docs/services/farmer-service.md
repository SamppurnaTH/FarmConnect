# Farmer Service

The **Farmer Service** manages the lifecycle of farmer profiles, from registration and KYC document verification to land detail management.

## 🌾 Core Responsibilities
- **Profile Management**: CRUD operations for farmer personal and land details.
- **KYC & Documents**: Secure storage and verification of identification and land ownership documents.
- **Verification Workflow**: Integration with identity and notification services for status updates.
- **Data Privacy**: AES-256 encryption of sensitive personal information (PII).

## 📡 API Endpoints

### Registration & Profile
| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/farmers/register` | Register a new farmer and create identity. | No |
| `GET` | `/farmers/{id}` | Retrieve farmer profile by ID. | Yes |
| `PUT` | `/farmers/{id}` | Update farmer profile details. | Yes (Owner/Admin) |
| `GET` | `/farmers/user/{userId}` | Lookup farmer by identity user ID. | Yes |

### Document Management
| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/farmers/{id}/documents` | Upload KYC/Land document (Multipart). | Yes (Owner) |
| `GET` | `/farmers/{id}/documents` | List metadata for all documents. | Yes |
| `GET` | `/farmers/{id}/documents/{docId}` | Download specific document file. | Yes |
| `PUT` | `/farmers/{id}/documents/{docId}/verify` | Verify or reject a document. | Yes (Officer) |

## 🛠️ Configuration
- **Database**: PostgreSQL (`agrichain_farmer`)
- **Storage**: Local filesystem (Docker volume mapped)
- **Encryption**: AES-256 for PII
- **Resilience**: Resilience4j Circuit Breakers for Identity Service calls
- **Port**: `8082`

## 🔗 Dependencies
- `identity-service`: User account creation
- `notification-service`: Status update alerts
- `eureka-service`: Discovery
