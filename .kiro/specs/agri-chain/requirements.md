# Requirements Document

## Introduction

Agri Chain is a web-based platform for agricultural departments, cooperatives, and market boards to manage agricultural supply chains and market linkages. The system enables farmers to register, list crops, sell produce, and receive subsidies; traders to purchase and manage orders; and officials to monitor operations, ensure compliance, and manage programs. The platform prioritizes transparency, traceability, and efficiency through audit trails, dashboards, and analytics.

## Glossary

- **System**: The Agri Chain platform as a whole
- **Identity_Service**: The microservice responsible for authentication, authorization, and audit logging
- **Farmer_Service**: The microservice responsible for farmer registration and profile management
- **Crop_Service**: The microservice responsible for crop listings and market linkage
- **Transaction_Service**: The microservice responsible for transaction and payment tracking
- **Subsidy_Service**: The microservice responsible for subsidy program and disbursement management
- **Compliance_Service**: The microservice responsible for compliance tracking and audit execution
- **Reporting_Service**: The microservice responsible for dashboard generation and analytics
- **Notification_Service**: The microservice responsible for delivering alerts and notifications
- **Farmer**: A registered user who cultivates and sells agricultural produce
- **Trader**: A registered user who purchases agricultural produce from farmers
- **Market_Officer**: A registered official who validates crop listings and oversees transactions
- **Program_Manager**: A registered official who manages subsidy programs and tracks budgets
- **Administrator**: A registered user who manages system users, roles, and configurations
- **Compliance_Officer**: A registered official who ensures policy adherence and conducts audits
- **Government_Auditor**: A registered official who reviews reports and subsidy utilization
- **RBAC**: Role-Based Access Control — a method of restricting system access based on user roles
- **CropListing**: A record created by a Farmer advertising a quantity of produce for sale at a stated price
- **Order**: A record created by a Trader to purchase a quantity from a CropListing
- **Transaction**: A financial record linking an Order to a payment event
- **Disbursement**: A record of subsidy funds transferred to a Farmer under a SubsidyProgram
- **ComplianceRecord**: A record capturing the result of a compliance check against a system entity
- **Audit**: A formal review conducted by a Compliance_Officer or Government_Auditor
- **KPI**: Key Performance Indicator — a measurable value used to evaluate system or program performance

---

## Requirements

### Requirement 1: User Authentication and Session Management

**User Story:** As a user of any role, I want to securely log in and have my session managed, so that only authorized individuals can access the system.

#### Acceptance Criteria

1. WHEN a user submits valid credentials, THE Identity_Service SHALL authenticate the user and issue a session token within 2 seconds.
2. WHEN a user submits invalid credentials, THE Identity_Service SHALL reject the request and return an error message indicating authentication failure without revealing which field was incorrect.
3. WHEN a session token expires after 30 minutes of inactivity, THE Identity_Service SHALL invalidate the token and require the user to re-authenticate.
4. WHEN a user explicitly logs out, THE Identity_Service SHALL immediately invalidate the session token.
5. IF a user submits 5 consecutive failed login attempts within 10 minutes, THEN THE Identity_Service SHALL lock the account and notify the registered email address.

---

### Requirement 2: Role-Based Access Control

**User Story:** As an Administrator, I want each user role to have access only to permitted resources, so that data integrity and security are maintained.

#### Acceptance Criteria

1. THE Identity_Service SHALL enforce RBAC such that each API endpoint is accessible only to users whose role has explicit permission for that endpoint.
2. WHEN a user attempts to access a resource outside their role's permissions, THE Identity_Service SHALL return an authorization error and log the attempt.
3. THE Administrator SHALL assign, modify, and revoke role assignments for any user account.
4. WHEN a role assignment is changed, THE Identity_Service SHALL apply the new permissions within 1 minute without requiring the affected user to re-authenticate.

---

### Requirement 3: Audit Logging

**User Story:** As a Compliance_Officer or Government_Auditor, I want every state-changing action to be recorded, so that a complete and tamper-evident audit trail is available.

#### Acceptance Criteria

1. WHEN any user performs a create, update, or delete operation on any system entity, THE Identity_Service SHALL record an audit log entry containing the UserID, action type, affected resource identifier, and UTC timestamp.
2. THE Identity_Service SHALL retain audit log entries for a minimum of 7 years.
3. WHEN a Compliance_Officer or Government_Auditor queries the audit log, THE Identity_Service SHALL return matching entries within 5 seconds for result sets up to 10,000 records.
4. THE Identity_Service SHALL store audit log entries in an append-only manner such that no user role can modify or delete existing entries.

---

### Requirement 4: Farmer Registration

**User Story:** As a Farmer, I want to register my profile on the platform, so that I can access market and subsidy services.

#### Acceptance Criteria

1. WHEN a Farmer submits a registration form with name, date of birth, gender, address, contact information, and land details, THE Farmer_Service SHALL create a Farmer record with status set to "Pending Verification".
2. WHEN a Farmer submits a registration form missing any mandatory field, THE Farmer_Service SHALL reject the submission and return a list of missing fields.
3. WHEN a Market_Officer approves a Farmer registration, THE Farmer_Service SHALL update the Farmer status to "Active" and notify the Farmer via the Notification_Service.
4. IF a Farmer submits a registration using contact information already associated with an existing Active Farmer record, THEN THE Farmer_Service SHALL reject the submission and return a duplicate-record error.

---

### Requirement 5: Farmer Document Verification

**User Story:** As a Market_Officer, I want to review and verify documents submitted by farmers, so that only legitimate farmers are approved on the platform.

#### Acceptance Criteria

1. WHEN a Farmer uploads a document of a supported type (national ID, land title, or tax certificate), THE Farmer_Service SHALL store the document and set its VerificationStatus to "Pending".
2. WHEN a Market_Officer marks a document as verified, THE Farmer_Service SHALL update the document VerificationStatus to "Verified" and record the reviewing officer's UserID and timestamp.
3. WHEN a Market_Officer rejects a document, THE Farmer_Service SHALL update the VerificationStatus to "Rejected", record a rejection reason, and notify the Farmer via the Notification_Service.
4. WHILE a Farmer's mandatory documents remain unverified, THE Farmer_Service SHALL restrict the Farmer from creating CropListings.

---

### Requirement 6: Farmer Profile Management

**User Story:** As a Farmer, I want to update my profile information, so that my records remain accurate.

#### Acceptance Criteria

1. WHEN an Active Farmer submits updated profile information, THE Farmer_Service SHALL apply the changes and record the previous values in the audit log via the Identity_Service.
2. WHEN a Farmer updates contact information, THE Farmer_Service SHALL require re-verification of the updated contact field before the new value is used for notifications.
3. THE Farmer_Service SHALL allow an Administrator to deactivate a Farmer account, setting the status to "Inactive" and preventing further logins for that account.

---

### Requirement 7: Crop Listing Creation

**User Story:** As a Farmer, I want to list my crops for sale on the platform, so that traders can discover and purchase my produce.

#### Acceptance Criteria

1. WHEN an Active Farmer with all mandatory documents verified submits a crop listing with crop type, quantity, price per unit, and location, THE Crop_Service SHALL create a CropListing record with status "Pending Approval".
2. WHEN a Farmer submits a crop listing with a quantity of zero or a price per unit of zero or less, THE Crop_Service SHALL reject the submission and return a validation error.
3. WHEN a Market_Officer approves a CropListing, THE Crop_Service SHALL update the listing status to "Active" and make it visible to Traders.
4. WHEN a Market_Officer rejects a CropListing, THE Crop_Service SHALL update the listing status to "Rejected", record the rejection reason, and notify the Farmer via the Notification_Service.
5. WHEN a Farmer updates an Active CropListing's quantity or price, THE Crop_Service SHALL record the previous values in the audit log and apply the changes immediately.

---

### Requirement 8: Crop Listing Discovery and Order Placement

**User Story:** As a Trader, I want to browse active crop listings and place orders, so that I can purchase agricultural produce.

#### Acceptance Criteria

1. WHEN a Trader queries crop listings with optional filters for crop type, location, or price range, THE Crop_Service SHALL return all Active CropListings matching the filters within 3 seconds.
2. WHEN a Trader places an order specifying a ListingID and quantity, THE Crop_Service SHALL verify that the requested quantity does not exceed the available quantity on the CropListing before creating an Order record with status "Pending".
3. IF the requested order quantity exceeds the available CropListing quantity, THEN THE Crop_Service SHALL reject the order and return an insufficient-quantity error.
4. WHEN an Order is created, THE Crop_Service SHALL notify the Farmer of the new order via the Notification_Service.
5. WHEN a Farmer accepts an Order, THE Crop_Service SHALL update the Order status to "Confirmed" and reduce the CropListing's available quantity by the ordered amount.
6. WHEN a Farmer declines an Order, THE Crop_Service SHALL update the Order status to "Declined" and notify the Trader via the Notification_Service.

---

### Requirement 9: Transaction Recording

**User Story:** As a Trader or Farmer, I want every order to generate a transaction record, so that financial activity is fully traceable.

#### Acceptance Criteria

1. WHEN an Order status is updated to "Confirmed", THE Transaction_Service SHALL create a Transaction record linked to the OrderID with status "Pending Payment".
2. THE Transaction_Service SHALL record the transaction amount as the product of the confirmed order quantity and the CropListing price per unit at the time of confirmation.
3. WHEN a Transaction is created, THE Transaction_Service SHALL notify both the Farmer and the Trader via the Notification_Service.

---

### Requirement 10: Payment Processing and Settlement

**User Story:** As a Trader, I want to submit payment for a confirmed transaction, so that the Farmer receives funds and the order is fulfilled.

#### Acceptance Criteria

1. WHEN a Trader submits a payment for a Transaction using a supported payment method (bank transfer, mobile money, or card), THE Transaction_Service SHALL create a Payment record with status "Processing".
2. WHEN a payment gateway confirms successful payment, THE Transaction_Service SHALL update the Payment status to "Completed" and the Transaction status to "Settled".
3. IF a payment gateway returns a failure response, THEN THE Transaction_Service SHALL update the Payment status to "Failed" and notify the Trader via the Notification_Service with the failure reason.
4. WHEN a Transaction is settled, THE Transaction_Service SHALL notify the Farmer via the Notification_Service.
5. WHILE a Transaction status is "Pending Payment", THE Transaction_Service SHALL allow the Trader to submit a payment within 48 hours before the Transaction is automatically cancelled.

---

### Requirement 11: Subsidy Program Management

**User Story:** As a Program_Manager, I want to create and manage subsidy programs, so that eligible farmers receive financial support.

#### Acceptance Criteria

1. WHEN a Program_Manager creates a subsidy program with a title, description, start date, end date, and budget amount, THE Subsidy_Service SHALL create a SubsidyProgram record with status "Draft".
2. WHEN a Program_Manager activates a SubsidyProgram, THE Subsidy_Service SHALL update the status to "Active" and make the program available for disbursement processing.
3. WHEN a Program_Manager closes a SubsidyProgram, THE Subsidy_Service SHALL update the status to "Closed" and prevent new disbursements from being created under that program.
4. WHILE a SubsidyProgram is Active, THE Subsidy_Service SHALL track the total disbursed amount and ensure it does not exceed the program budget.
5. IF a disbursement request would cause the total disbursed amount to exceed the SubsidyProgram budget, THEN THE Subsidy_Service SHALL reject the disbursement and return a budget-exceeded error.

---

### Requirement 12: Subsidy Disbursement

**User Story:** As a Program_Manager, I want to disburse subsidy funds to eligible farmers, so that program benefits reach the intended recipients.

#### Acceptance Criteria

1. WHEN a Program_Manager creates a disbursement for an Active Farmer under an Active SubsidyProgram with a specified amount, THE Subsidy_Service SHALL create a Disbursement record with status "Pending".
2. WHEN a disbursement is approved by an authorized approver, THE Subsidy_Service SHALL update the Disbursement status to "Approved" and initiate fund transfer.
3. WHEN a fund transfer is confirmed, THE Subsidy_Service SHALL update the Disbursement status to "Disbursed" and notify the Farmer via the Notification_Service.
4. IF a fund transfer fails, THEN THE Subsidy_Service SHALL update the Disbursement status to "Failed" and notify the Program_Manager via the Notification_Service.
5. THE Subsidy_Service SHALL prevent creation of a duplicate Disbursement for the same FarmerID and ProgramID within the same program cycle.

---

### Requirement 13: Compliance Record Management

**User Story:** As a Compliance_Officer, I want to record and track compliance checks against system entities, so that policy adherence is documented.

#### Acceptance Criteria

1. WHEN a Compliance_Officer creates a compliance record for an entity with entity type, check result (Pass or Fail), date, and notes, THE Compliance_Service SHALL store the ComplianceRecord and link it to the specified entity.
2. WHEN a compliance check result is "Fail", THE Compliance_Service SHALL notify the Administrator and the relevant entity owner via the Notification_Service.
3. THE Compliance_Service SHALL allow a Compliance_Officer to query all ComplianceRecords for a given entity, returning results within 3 seconds.

---

### Requirement 14: Audit Execution

**User Story:** As a Compliance_Officer or Government_Auditor, I want to conduct formal audits and record findings, so that systemic issues are identified and addressed.

#### Acceptance Criteria

1. WHEN a Compliance_Officer or Government_Auditor initiates an audit with a defined scope, THE Compliance_Service SHALL create an Audit record with status "In Progress".
2. WHEN an auditor submits findings for an In Progress audit, THE Compliance_Service SHALL update the Audit record with the findings and set the status to "Completed".
3. WHEN an Audit is completed, THE Compliance_Service SHALL notify the Administrator via the Notification_Service.
4. THE Compliance_Service SHALL allow a Government_Auditor to export a completed Audit record as a PDF report within 10 seconds of the export request.

---

### Requirement 15: Reporting and Dashboard

**User Story:** As a Program_Manager, Market_Officer, or Government_Auditor, I want to view dashboards and generate reports, so that I can monitor system performance and program outcomes.

#### Acceptance Criteria

1. WHEN an authorized user requests a dashboard, THE Reporting_Service SHALL render KPI summaries including total active farmers, total crop listings, total transaction volume, and total subsidy disbursed within 5 seconds.
2. WHEN an authorized user requests a report with a defined scope and date range, THE Reporting_Service SHALL generate the report and return it within 30 seconds for date ranges up to 12 months.
3. THE Reporting_Service SHALL allow authorized users to export reports in CSV and PDF formats.
4. WHEN a report is generated, THE Reporting_Service SHALL record the report metadata including scope, generating user, and generation timestamp.

---

### Requirement 16: Notifications and Alerts

**User Story:** As any registered user, I want to receive timely notifications about events relevant to my role, so that I can take action without manually polling the system.

#### Acceptance Criteria

1. WHEN the Notification_Service receives a notification request from any service, THE Notification_Service SHALL deliver the notification via the channel specified in the request (in-app, SMS, or email) within 60 seconds.
2. WHEN an in-app notification is delivered, THE Notification_Service SHALL set the notification status to "Delivered" and record the delivery timestamp.
3. IF a notification delivery attempt fails, THEN THE Notification_Service SHALL retry delivery up to 3 times at 5-minute intervals before setting the notification status to "Failed".
4. WHEN a user reads an in-app notification, THE Notification_Service SHALL update the notification status to "Read".
5. THE Notification_Service SHALL allow a user to query their own notification history, returning results paginated at 50 records per page.

---

### Requirement 17: System Performance

**User Story:** As a system operator, I want the platform to handle high concurrency, so that nationwide deployment remains responsive under load.

#### Acceptance Criteria

1. WHILE the System is under a load of up to 200,000 concurrent users, THE System SHALL maintain an API response time of 2 seconds or less for all read operations.
2. WHILE the System is under a load of up to 200,000 concurrent users, THE System SHALL maintain an API response time of 5 seconds or less for all write operations.
3. THE System SHALL achieve 99.9% uptime measured on a rolling 30-day basis, excluding scheduled maintenance windows communicated at least 24 hours in advance.

---

### Requirement 18: Data Security

**User Story:** As a system operator, I want all sensitive data to be protected in transit and at rest, so that user privacy and regulatory compliance are maintained.

#### Acceptance Criteria

1. THE System SHALL encrypt all data in transit using TLS 1.2 or higher.
2. THE System SHALL encrypt all personally identifiable information stored in the database using AES-256 or an equivalent standard.
3. THE Identity_Service SHALL hash all user passwords using a salted hashing algorithm before storage, such that plaintext passwords are never persisted.
4. WHEN a security vulnerability is identified in a system dependency, THE System SHALL apply the relevant patch within 30 days of the vulnerability's public disclosure.
