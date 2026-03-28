# Implementation Plan: Agri Chain

## Overview

Implement the Agri Chain platform as a set of Spring Boot microservices (Java) behind an API Gateway. Each service owns its domain, exposes REST endpoints, and integrates with shared infrastructure (PostgreSQL, object storage, notification channels, audit log). Property-based tests use jqwik; unit tests use JUnit 5 + Mockito.

## Tasks

- [x] 1. Project scaffolding and shared infrastructure
  - Create a multi-module Maven/Gradle project with one module per service: `identity-service`, `farmer-service`, `crop-service`, `transaction-service`, `subsidy-service`, `compliance-service`, `reporting-service`, `notification-service`, and a `common` module for shared types
  - Define shared domain types: UUIDs, enums (`UserRole`, `FarmerStatus`, `ListingStatus`, `OrderStatus`, `TransactionStatus`, `PaymentStatus`, `SubsidyProgramStatus`, `DisbursementStatus`, `NotificationChannel`, `NotificationStatus`, `VerificationStatus`, `AuditStatus`)
  - Configure Spring Data JPA entities and repositories for all data models defined in the design document
  - Set up Flyway/Liquibase migration scripts for all tables including the append-only `audit_log` table
  - Configure AES-256 encryption utility for PII fields (name, date_of_birth, address, contact_info, email)
  - _Requirements: 4.1, 6.1, 18.2_

- [x] 2. Identity Service — authentication and session management
  - [x] 2.1 Implement `POST /auth/login` — validate credentials, issue short-lived JWT, store token reference in token store
    - Hash comparison using salted algorithm (BCrypt); never persist plaintext
    - Return `401` for invalid credentials without revealing which field failed
    - _Requirements: 1.1, 1.2, 18.3_
  - [x] 2.2 Write property test for valid credentials always producing a token (Property 1)
    - **Property 1: Valid credentials always produce a session token**
    - **Validates: Requirements 1.1**
  - [x] 2.3 Write property test for invalid credentials returning a generic error (Property 2)
    - **Property 2: Invalid credentials never reveal which field was wrong**
    - **Validates: Requirements 1.2**
  - [x] 2.4 Implement `POST /auth/logout` and `POST /auth/refresh` — invalidate/refresh token in token store
    - _Requirements: 1.3, 1.4_
  - [x] 2.5 Write property test for invalidated tokens being rejected (Property 3)
    - **Property 3: Invalidated tokens are always rejected**
    - **Validates: Requirements 1.3, 1.4**
  - [x] 2.6 Implement account lockout logic — track consecutive failures, lock after 5 within 10 minutes, trigger email notification
    - _Requirements: 1.5_
  - [x] 2.7 Write property test for account lockout after repeated failures (Property 4)
    - **Property 4: Account lockout after repeated failures**
    - **Validates: Requirements 1.5**
  - [x] 2.8 Implement `PUT /roles/{userId}` — Administrator-only role assignment; apply new permissions within 1 minute
    - _Requirements: 2.3, 2.4_
  - [x]* 2.9 Write property test for role changes being reflected in subsequent requests (Property 7)
    - **Property 7: Role changes are reflected in subsequent requests**
    - **Validates: Requirements 2.3, 2.4**
  - [x] 2.10 Implement RBAC filter/interceptor — reject requests from roles without explicit endpoint permission, log the attempt
    - _Requirements: 2.1, 2.2_
  - [x]* 2.11 Write property test for RBAC rejecting all unauthorized endpoint access (Property 5)
    - **Property 5: RBAC rejects all unauthorized endpoint access**
    - **Validates: Requirements 2.1, 2.2**
  - [x]* 2.12 Write property test for unauthorized access attempts always being logged (Property 6)
    - **Property 6: Unauthorized access attempts are always logged**
    - **Validates: Requirements 2.2**
  - [x] 2.13 Implement `GET /audit-log` — paginated query for Compliance_Officer and Government_Auditor; enforce append-only constraint at DB level (no UPDATE/DELETE on audit_log)
    - _Requirements: 3.1, 3.2, 3.3, 3.4_
  - [x]* 2.14 Write property test for every state-changing operation producing an audit log entry (Property 8)
    - **Property 8: Every state-changing operation produces an audit log entry**
    - **Validates: Requirements 3.1, 6.1, 7.5**
  - [x]* 2.15 Write property test for audit log entries being immutable (Property 9)
    - **Property 9: Audit log entries are immutable**
    - **Validates: Requirements 3.4**
  - [x] 2.16 Write property test for passwords never stored as plaintext (Property 36)
    - **Property 36: Passwords are never stored as plaintext**
    - **Validates: Requirements 18.3**

- [x] 3. Checkpoint — Identity Service
  - Ensure all Identity Service tests pass, ask the user if questions arise.

- [x] 4. Farmer Service — registration and document verification
  - [x] 4.1 Implement `POST /farmers` — validate all mandatory fields, create Farmer with `Pending_Verification`, reject duplicates on contact_info
    - _Requirements: 4.1, 4.2, 4.4_
  - [x]* 4.2 Write property test for valid registration creating a Pending Verification record (Property 10)
    - **Property 10: Valid farmer registration creates a Pending Verification record**
    - **Validates: Requirements 4.1**
  - [x]* 4.3 Write property test for incomplete registration being rejected with missing field list (Property 11)
    - **Property 11: Incomplete registration is rejected with missing field list**
    - **Validates: Requirements 4.2**
  - [x]* 4.4 Write property test for duplicate contact information being rejected (Property 12)
    - **Property 12: Duplicate contact information is rejected**
    - **Validates: Requirements 4.4**
  - [x] 4.5 Implement `PUT /farmers/{id}/status` — Market_Officer approves (→ Active) or Administrator deactivates (→ Inactive); notify Farmer on approval
    - _Requirements: 4.3, 6.3_
  - [x] 4.6 Implement `GET /farmers/{id}` and `PUT /farmers/{id}` — profile view/update; record previous values in audit log; require re-verification on contact_info change
    - _Requirements: 6.1, 6.2_
  - [x]* 4.7 Write property test for contact info update requiring re-verification before notification use (Property 16)
    - **Property 16: Contact info update requires re-verification before notification use**
    - **Validates: Requirements 6.2**
  - [x] 4.8 Implement `POST /farmers/{id}/documents` — store document in object storage, create FarmerDocument with `Pending` status
    - _Requirements: 5.1_
  - [x]* 4.9 Write property test for document upload setting VerificationStatus to Pending (Property 13)
    - **Property 13: Document upload sets VerificationStatus to Pending**
    - **Validates: Requirements 5.1**
  - [x] 4.10 Implement `PUT /farmers/{id}/documents/{docId}/verify` — Market_Officer approves or rejects; record reviewer UserID and timestamp; notify Farmer on rejection
    - _Requirements: 5.2, 5.3_
  - [x]* 4.11 Write property test for document verification recording reviewer identity and timestamp (Property 14)
    - **Property 14: Document verification records reviewer identity and timestamp**
    - **Validates: Requirements 5.2, 5.3**

- [x] 5. Checkpoint — Farmer & Crops Service
  - Ensure all Farmer & Crops Service tests pass, ask the user if questions arise.

- [x] 6. Crop Service — listings and orders
  - [x] 6.1 Implement `POST /listings` — verify Farmer is Active and all mandatory documents are Verified; validate quantity > 0 and price_per_unit > 0; create CropListing with `Pending_Approval`
    - _Requirements: 5.4, 7.1, 7.2_
  - [x]* 6.2 Write property test for farmers with unverified documents being blocked from creating listings (Property 15)
    - **Property 15: Farmers with unverified documents cannot create crop listings**
    - **Validates: Requirements 5.4**
  - [x]* 6.3 Write property test for valid crop listing being created with Pending Approval status (Property 17)
    - **Property 17: Valid crop listing is created with Pending Approval status**
    - **Validates: Requirements 7.1**
  - [x]* 6.4 Write property test for zero or negative quantity/price being rejected (Property 18)
    - **Property 18: Zero or negative quantity/price is rejected**
    - **Validates: Requirements 7.2**
  - [x] 6.5 Implement `PUT /listings/{id}` — Market_Officer approves (→ Active) or rejects (→ Rejected, record reason, notify Farmer); Farmer updates quantity/price (record previous values in audit log)
    - _Requirements: 7.3, 7.4, 7.5_
  - [x] 6.6 Implement `GET /listings` — return only Active listings matching optional filters (crop type, location, price range) within 3 seconds
    - _Requirements: 8.1_
  - [x]* 6.7 Write property test for listing filter returning only Active listings matching all criteria (Property 19)
    - **Property 19: Crop listing filter returns only Active listings matching all criteria**
    - **Validates: Requirements 8.1**
  - [x] 6.8 Implement `POST /listings/{id}/orders` — validate requested quantity ≤ available quantity; create Order with `Pending`; notify Farmer
    - _Requirements: 8.2, 8.3, 8.4_
  - [x]* 6.9 Write property test for order quantity being validated against available listing quantity (Property 20)
    - **Property 20: Order quantity is validated against available listing quantity**
    - **Validates: Requirements 8.2, 8.3**
  - [x] 6.10 Implement `PUT /orders/{id}/accept` — update Order to `Confirmed`, reduce CropListing available quantity; trigger Transaction_Service to create Transaction
    - _Requirements: 8.5, 9.1_
  - [x]* 6.11 Write property test for farmer acceptance reducing available listing quantity (Property 21)
    - **Property 21: Farmer acceptance reduces available listing quantity**
    - **Validates: Requirements 8.5**
  - [x] 6.12 Implement `PUT /orders/{id}/decline` — update Order to `Declined`; notify Trader
    - _Requirements: 8.6_

- [x] 7. Checkpoint — Crop Service
  - Ensure all Crop Service tests pass, ask the user if questions arise.

- [x] 8. Trader Service — buyer registration and order placement
  - [x] 8.1 Implement `POST /traders` — Public registration for Traders; create User in Identity_Service; create Trader profile
    - _Requirements: 11.1_
  - [x] 8.2 Implement `GET /traders/{id}/orders` — Trader view own orders (query Crop_Service)
    - _Requirements: 11.3_

- [x] 9. Transaction Service — recording and payment
  - [x] 9.1 Implement Transaction creation on Order confirmation — create Transaction with `Pending_Payment`, amount = quantity × price_per_unit at confirmation time, set expires_at = created_at + 48h; notify Farmer and Trader
    - _Requirements: 9.1, 9.2, 9.3_
  - [x]* 9.2 Write property test for confirmed order creating a Transaction with correct amount (Property 22)
    - **Property 22: Confirmed order creates a Transaction with correct amount**
    - **Validates: Requirements 9.1, 9.2**
  - [x] 9.3 Implement `POST /transactions/{id}/payments` — validate Transaction is in `Pending_Payment` and not expired; create Payment with `Processing`; submit to payment gateway
    - _Requirements: 10.1, 10.5_
  - [x] 9.4 Implement payment gateway webhook handler — on success: set Payment → `Completed`, Transaction → `Settled`, notify Farmer; on failure: set Payment → `Failed`, record failure_reason, notify Trader
    - _Requirements: 10.2, 10.3, 10.4_
  - [x]* 9.5 Write property test for payment gateway success settling the transaction (Property 23)
    - **Property 23: Payment gateway success settles the transaction**
    - **Validates: Requirements 10.2**
  - [x]* 9.6 Write property test for payment gateway failure marking payment as failed (Property 24)
    - **Property 24: Payment gateway failure marks payment as failed**
    - **Validates: Requirements 10.3**
  - [x] 9.7 Implement scheduled job to auto-cancel Transactions in `Pending_Payment` whose `expires_at` has passed
    - _Requirements: 10.5_
  - [x]* 9.8 Write property test for pending payment being auto-cancelled after 48 hours (Property 25)
    - **Property 25: Pending payment is auto-cancelled after 48 hours**
    - **Validates: Requirements 10.5**
  - [x] 9.9 Implement `GET /transactions` and `GET /payments/{id}` — role-filtered query endpoints
    - _Requirements: 9.1_

- [x] 10. Checkpoint — Transaction Service
  - Ensure all Transaction Service tests pass, ask the user if questions arise.

- [x] 11. Subsidy Service — programs and disbursements
  - [x] 11.1 Implement `POST /programs` — create SubsidyProgram with `Draft` status
    - _Requirements: 11.1_
  - [x] 11.2 Implement `PUT /programs/{id}/activate` and `PUT /programs/{id}/close` — enforce monotonic Draft → Active → Closed transitions; prevent new disbursements under Closed programs
    - _Requirements: 11.2, 11.3_
  - [x]* 11.3 Write property test for subsidy program status transitions being monotonic (Property 26)
    - **Property 26: Subsidy program status transitions are monotonic**
    - **Validates: Requirements 11.1, 11.2, 11.3**
  - [x] 11.4 Implement `POST /programs/{id}/disbursements` — validate program is Active, farmer is Active, amount would not exceed budget; create Disbursement with `Pending`; enforce unique (farmer_id, program_id, program_cycle)
    - _Requirements: 11.4, 11.5, 12.1, 12.5_
  - [x]* 11.5 Write property test for total disbursed never exceeding program budget (Property 27)
    - **Property 27: Total disbursed never exceeds program budget**
    - **Validates: Requirements 11.4, 11.5**
  - [x]* 11.6 Write property test for duplicate disbursement for same farmer/program/cycle being rejected (Property 28)
    - **Property 28: Duplicate disbursement for same farmer/program/cycle is rejected**
    - **Validates: Requirements 12.5**
  - [x] 11.7 Implement `PUT /disbursements/{id}/approve` — update Disbursement to `Approved`, initiate fund transfer; on transfer success → `Disbursed` + notify Farmer; on failure → `Failed` + notify Program_Manager
    - _Requirements: 12.2, 12.3, 12.4_

- [x] 12. Checkpoint — Subsidy Service
  - Ensure all Subsidy Service tests pass, ask the user if questions arise.

- [x] Task 13: Compliance Service — records and audits (Requirement 13-14)
  - [x] 12.1 Implement `POST /compliance-records` and `GET /compliance-records` — create and query ComplianceRecords; notify Administrator and entity owner on Fail result
    - _Requirements: 13.1, 13.2, 13.3_
  - [x]* 12.2 Write property test for compliance record being linked to the specified entity (Property 29)
    - **Property 29: Compliance record is linked to the specified entity**
    - **Validates: Requirements 13.1**
  - [x] 12.3 Implement `POST /audits` and `PUT /audits/{id}/findings` — create Audit with `In_Progress`; submit findings → `Completed`; notify Administrator on completion
    - _Requirements: 14.1, 14.2, 14.3_
  - [x]* 12.4 Write property test for audit record transitioning correctly through its lifecycle (Property 30)
    - **Property 30: Audit record transitions correctly through its lifecycle**
    - **Validates: Requirements 14.1, 14.2**
  - [x] 12.5 Implement `GET /audits/{id}/export` — generate and return PDF of completed Audit within 10 seconds
    - _Requirements: 14.4_

- [x] 13. Reporting Service — dashboards and exports (Requirement 15)
  - [x] 13.1 Implement `GET /dashboard` — aggregate KPIs (total active farmers, total crop listings, total transaction volume, total subsidy disbursed) from read replicas; respond within 5 seconds
    - _Requirements: 15.1_
  - [x]* 13.2 Write property test for dashboard response including all required KPI fields (Property 31)
    - **Property 31: Dashboard response includes all required KPI fields**
    - **Validates: Requirements 15.1**
  - [x] 13.3 Implement `POST /reports` and `GET /reports/{id}/export` — generate report for defined scope and date range (≤12 months within 30 seconds); persist report metadata; export as CSV or PDF
    - _Requirements: 15.2, 15.3, 15.4_
  - [x]* 13.4 Write property test for report metadata being persisted on generation (Property 32)
    - **Property 32: Report metadata is persisted on generation**
    - **Validates: Requirements 15.4**

- [x] 14. Notification Service — delivery and history (Requirement 16)
  - [x] 14.1 Implement `POST /notifications` (internal) — deliver notification via specified channel (In_App, SMS, Email) within 60 seconds; set status to `Delivered` on success with delivery timestamp
    - _Requirements: 16.1, 16.2_
  - [x]* 14.2 Write property test for notification being delivered via the channel specified in the request (Property 33)
    - **Property 33: Notification delivered via the channel specified in the request**
    - **Validates: Requirements 16.1**
  - [x] 14.3 Implement retry logic — on delivery failure, retry up to 3 times at 5-minute intervals; after exhaustion set status to `Failed`
    - _Requirements: 16.3_
  - [x]* 14.4 Write property test for notification retry count never exceeding 3 (Property 34)
    - **Property 34: Notification retry count never exceeds 3**
    - **Validates: Requirements 16.3**
  - [x] 14.5 Implement `GET /notifications/me` (paginated, 50 per page) and `PUT /notifications/{id}/read`
    - _Requirements: 16.4, 16.5_
  - [x]* 14.6 Write property test for notification history pages containing at most 50 records (Property 35)
    - **Property 35: Notification history pages contain at most 50 records**
    - **Validates: Requirements 16.5**

- [x] 15. Final checkpoint — full integration
  - Ensure all tests pass across all services, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Property-based tests use jqwik (`@Property(tries = 100)`) with tag format: `// Feature: agri-chain, Property {N}: {property_text}`
- Unit tests use JUnit 5 + Mockito for specific examples, edge cases, and integration points
- All services emit structured logs; inter-service calls use circuit breakers (e.g., Resilience4j)
- Reporting queries run against read replicas to avoid impacting transactional workloads
