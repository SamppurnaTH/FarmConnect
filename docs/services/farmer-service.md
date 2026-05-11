# Farmer Service: Producer Lifecycle Management

The **Farmer Service** is the primary domain service for managing the 5,000+ farmers in the FarmConnect ecosystem. It handles everything from digital onboarding to complex land verification workflows.

## 🌾 Domain Business Logic

Farmer registration is a coordinated process between the **Farmer Service** and **Identity Service**:
1. User submits profile data + credentials via the Gateway Service.
2. Farmer Service calls Identity Service (via service discovery) to create a `FARMER` account.
3. On success, a local `Farmer` profile is created with status `PENDING_VERIFICATION`.
4. A notification is triggered to the Market Officer via Notification Service (via service discovery).

### 2. KYC & Document Verification
Farmers must upload two types of documents:
- **Identification**: National ID, Passport, or Voter ID.
- **Land Ownership**: Deeds, Tax Receipts, or Surveyor Reports.
- **Verification Logic**: Only a `MARKET_OFFICER` can transition a farmer to `VERIFIED` status after reviewing these documents in the Officer Dashboard.

### 3. PII Security (Encryption)
To comply with data protection regulations, sensitive fields are encrypted at rest using **AES-256 GCM**:
- `Name`, `Date of Birth`, `Address`, and `Contact Info` are encrypted/decrypted transparently via JPA Attribute Converters.

## 📡 API Specification

### Farmer Profiles
- **`GET /farmers/me`**: Context-aware profile retrieval. Automatically identifies the logged-in farmer via JWT.
- **`GET /farmers/{id}`**: Detailed view for Officers.
- **`GET /farmers?status=&search=`**: Paginated list for administrative oversight.

### Document Management System
| Endpoint | Method | Input | Purpose |
| :--- | :--- | :--- | :--- |
| `/farmers/{id}/documents/upload` | `POST` | `MultipartFile` | Uploads binary to secure local storage. |
| `/farmers/{id}/documents/{docId}/verify` | `PUT` | `{"status", "reason"}` | Updates verification status. Triggers notification to farmer. |

## 🛡️ Resilience & Fault Tolerance
The service integrates **Resilience4j** to handle downstream dependencies:
- **Circuit Breaker**: If Identity Service is down, registration attempts are gracefully rejected with a "Service Temporarily Unavailable" message instead of timing out.
- **Retries**: Automatic retries for Notification Service calls to ensure farmers receive their status updates even during brief network blips.

## 🛠️ Infrastructure
- **Storage**: Documents are stored in a dedicated Docker Volume (`farmer_documents`) to persist across container restarts.
- **Database**: PostgreSQL (`agrichain_farmer`).
- **Encryption Key**: Managed via the `ENCRYPTION_KEY` environment variable.
