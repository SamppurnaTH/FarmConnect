# Requirements Document

## Introduction

The Agri Chain Frontend is a single-page application (SPA) that provides the user interface for the Agri Chain platform. It communicates exclusively with the existing Spring Boot microservices backend via REST APIs through the API Gateway. The frontend must serve all six user roles — Farmer, Trader, Market_Officer, Program_Manager, Compliance_Officer, and Government_Auditor — as well as the Administrator, each with a tailored experience exposing only the features and data relevant to their role. The application must be responsive, accessible, and secure, reflecting the same business rules enforced by the backend.

## Glossary

- **SPA**: Single-Page Application — a web application that loads a single HTML page and dynamically updates content without full page reloads
- **Frontend**: The Agri Chain SPA client application
- **API_Gateway**: The backend entry point that routes requests to microservices and enforces JWT validation
- **JWT**: JSON Web Token — the session token issued by the Identity_Service and sent with every authenticated request
- **Auth_Module**: The frontend module responsible for login, logout, token storage, and session management
- **Route_Guard**: A frontend mechanism that prevents navigation to routes the current user's role is not permitted to access
- **Farmer**: A registered user who cultivates and sells agricultural produce
- **Trader**: A registered user who purchases agricultural produce from farmers
- **Market_Officer**: A registered official who validates crop listings and oversees transactions
- **Program_Manager**: A registered official who manages subsidy programs and tracks budgets
- **Administrator**: A registered user who manages system users, roles, and configurations
- **Compliance_Officer**: A registered official who ensures policy adherence and conducts audits
- **Government_Auditor**: A registered official who reviews reports and subsidy utilization
- **Dashboard**: A role-specific landing page displaying KPI summaries and recent activity
- **Notification_Panel**: A UI component that displays in-app notifications and their read/unread status
- **RBAC**: Role-Based Access Control — enforced both on the backend and reflected in the frontend by hiding or disabling unauthorized UI elements

---

## Requirements

### Requirement 1: Authentication and Session Management

**User Story:** As a user of any role, I want to log in with my credentials and have my session managed securely, so that I can access the platform and be automatically signed out when inactive.

#### Acceptance Criteria

1. WHEN a user submits valid credentials on the login page, THE Auth_Module SHALL send the credentials to `POST /auth/login`, store the returned JWT in memory (not localStorage), and redirect the user to their role-specific Dashboard within 2 seconds of receiving the response.
2. WHEN the backend returns an authentication error, THE Auth_Module SHALL display a generic error message that does not indicate which field was incorrect, and SHALL NOT clear the username field.
3. WHEN a user explicitly clicks the logout button, THE Auth_Module SHALL call `POST /auth/logout`, clear the stored JWT, and redirect the user to the login page.
4. WHEN the JWT is within 5 minutes of its 30-minute inactivity expiry, THE Auth_Module SHALL silently call `POST /auth/refresh` to extend the session without interrupting the user.
5. WHEN the JWT expires and a refresh attempt fails, THE Auth_Module SHALL redirect the user to the login page and display a session-expired message.
6. IF the backend returns a 423 Locked response on login, THEN THE Auth_Module SHALL display a message informing the user that their account is locked and to check their registered email.
7. THE Auth_Module SHALL attach the JWT as a Bearer token in the Authorization header of every authenticated API request.

---

### Requirement 2: Role-Based Navigation and Access Control

**User Story:** As any authenticated user, I want to see only the navigation items and pages relevant to my role, so that the interface is uncluttered and I cannot accidentally access unauthorized features.

#### Acceptance Criteria

1. WHEN a user authenticates, THE Frontend SHALL render a navigation menu containing only the routes permitted for that user's role, as defined in the role-permission matrix below.
2. THE Frontend SHALL implement Route_Guards such that navigating directly to a URL for a route outside the user's role permissions redirects the user to a 403 Forbidden page.
3. WHEN the backend returns a 403 response for any API call, THE Frontend SHALL display an access-denied message and SHALL NOT expose any partial data from the response.
4. THE Frontend SHALL hide or disable action buttons (e.g., "Approve", "Create Disbursement") for operations the current user's role is not permitted to perform.
5. WHEN an Administrator changes a user's role, THE Frontend SHALL reflect the updated permissions on the next login without requiring a code change or deployment.

**Role-Permission Matrix:**

| Route / Feature | Farmer | Trader | Market_Officer | Program_Manager | Administrator | Compliance_Officer | Government_Auditor |
|---|---|---|---|---|---|---|---|
| Dashboard | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Farmer Registration (public) | ✓ | — | — | — | — | — | — |
| My Profile | ✓ | ✓ | — | — | — | — | — |
| Farmer Management | — | — | ✓ | — | ✓ | — | — |
| My Crop Listings | ✓ | — | — | — | — | — | — |
| Browse Listings | — | ✓ | ✓ | — | — | — | — |
| My Orders (Farmer) | ✓ | — | — | — | — | — | — |
| My Orders (Trader) | — | ✓ | — | — | — | — | — |
| Transactions | ✓ | ✓ | ✓ | — | — | — | — |
| Subsidy Programs | — | — | — | ✓ | — | — | ✓ |
| Disbursements | — | — | — | ✓ | — | — | ✓ |
| Compliance Records | — | — | — | — | — | ✓ | — |
| Audits | — | — | — | — | — | ✓ | ✓ |
| Reports & Dashboard KPIs | — | — | ✓ | ✓ | — | — | ✓ |
| User Management | — | — | — | — | ✓ | — | — |
| Audit Log Viewer | — | — | — | — | — | ✓ | ✓ |
| Notifications | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |

---

### Requirement 3: Farmer Registration and Profile Management

**User Story:** As a Farmer, I want to register on the platform and manage my profile, so that I can access market and subsidy services.

#### Acceptance Criteria

1. THE Frontend SHALL provide a public registration form collecting name, date of birth, gender, address, contact information, and land details, and SHALL submit the data to `POST /farmers`.
2. WHEN the backend returns a validation error listing missing fields, THE Frontend SHALL highlight each missing field inline and display the field names in a summary error message.
3. WHEN the backend returns a duplicate-record error, THE Frontend SHALL display a message indicating that the contact information is already registered.
4. WHEN a Farmer's registration is successfully submitted, THE Frontend SHALL display a confirmation message stating that the registration is pending verification.
5. WHEN an Active Farmer views their profile, THE Frontend SHALL display all profile fields and provide an edit form that submits changes to `PUT /farmers/{id}`.
6. WHEN a Farmer updates their contact information, THE Frontend SHALL display a notice that the new contact value requires re-verification before it will be used for notifications.
7. THE Frontend SHALL allow a Farmer to upload documents (National ID, Land Title, Tax Certificate) via a file upload control that submits to `POST /farmers/{id}/documents`, and SHALL display the current VerificationStatus of each uploaded document.

---

### Requirement 4: Farmer Document Verification (Market Officer)

**User Story:** As a Market_Officer, I want to review farmer documents and approve or reject registrations, so that only legitimate farmers are active on the platform.

#### Acceptance Criteria

1. THE Frontend SHALL provide a Farmer Management list view for Market_Officers and Administrators showing farmers with status Pending_Verification, with the ability to view each farmer's submitted documents.
2. WHEN a Market_Officer views a farmer's document, THE Frontend SHALL display the document and provide "Verify" and "Reject" action buttons that call `PUT /farmers/{id}/documents/{docId}/verify`.
3. WHEN a Market_Officer clicks "Reject", THE Frontend SHALL require a rejection reason to be entered before submitting the request.
4. WHEN a Market_Officer approves a farmer registration, THE Frontend SHALL call `PUT /farmers/{id}/status` and display a success confirmation.
5. WHEN the backend confirms a status update, THE Frontend SHALL update the displayed farmer status without requiring a full page reload.

---

### Requirement 5: Crop Listing Management (Farmer)

**User Story:** As a Farmer, I want to create and manage my crop listings, so that traders can discover and purchase my produce.

#### Acceptance Criteria

1. THE Frontend SHALL provide a crop listing creation form for Active Farmers with verified documents, collecting crop type, quantity, price per unit, and location, and submitting to `POST /listings`.
2. WHEN the backend rejects a listing due to unverified documents, THE Frontend SHALL display a message explaining that all mandatory documents must be verified before creating a listing.
3. WHEN the backend returns a validation error for zero or negative quantity or price, THE Frontend SHALL highlight the offending fields and display the validation message.
4. THE Frontend SHALL display a list of the Farmer's own crop listings with their current status (Pending Approval, Active, Rejected, Closed).
5. WHEN a Farmer views an Active listing, THE Frontend SHALL provide an edit form for quantity and price that submits to `PUT /listings/{id}`.
6. WHEN a Farmer views their listings, THE Frontend SHALL display pending orders for each Active listing with "Accept" and "Decline" action buttons.
7. WHEN a Farmer accepts an order, THE Frontend SHALL call `PUT /orders/{id}/accept` and update the order status and listing available quantity in the UI without a full page reload.
8. WHEN a Farmer declines an order, THE Frontend SHALL call `PUT /orders/{id}/decline` and update the order status in the UI.

---

### Requirement 6: Crop Listing Discovery and Order Placement (Trader)

**User Story:** As a Trader, I want to browse active crop listings and place orders, so that I can purchase agricultural produce.

#### Acceptance Criteria

1. THE Frontend SHALL provide a listing browse page for Traders and Market_Officers that calls `GET /listings` and displays results in a paginated list or grid.
2. THE Frontend SHALL provide filter controls for crop type, location, and price range that update the listing query parameters and re-fetch results without a full page reload.
3. WHEN a Trader selects a listing, THE Frontend SHALL display the listing details and an order form with a quantity input field.
4. WHEN a Trader submits an order, THE Frontend SHALL call `POST /listings/{id}/orders` and display a success confirmation or the specific error returned by the backend.
5. IF the backend returns an insufficient-quantity error, THEN THE Frontend SHALL display a message stating the available quantity and SHALL NOT submit the order again automatically.
6. THE Frontend SHALL provide a Trader order history page that displays all orders placed by the Trader with their current status.

---

### Requirement 7: Transaction and Payment Management

**User Story:** As a Trader or Farmer, I want to view transactions and submit payments, so that financial activity is traceable and orders are fulfilled.

#### Acceptance Criteria

1. THE Frontend SHALL provide a transactions list page for Farmers, Traders, and Market_Officers that calls `GET /transactions` and displays transaction status, amount, and linked order details.
2. WHEN a Trader views a Transaction in Pending_Payment status, THE Frontend SHALL display a payment form with payment method selection (Bank Transfer, Mobile Money, Card) and a submit button that calls `POST /transactions/{id}/payments`.
3. THE Frontend SHALL display a countdown timer showing the remaining time before a Pending_Payment transaction expires (48 hours from creation).
4. WHEN the countdown reaches zero, THE Frontend SHALL update the transaction status to Cancelled in the UI and disable the payment form.
5. WHEN a payment is submitted, THE Frontend SHALL display a "Processing" status and poll `GET /payments/{id}` at 10-second intervals until the status changes to Completed or Failed.
6. WHEN a payment status becomes Failed, THE Frontend SHALL display the failure reason returned by the backend.

---

### Requirement 8: Subsidy Program and Disbursement Management

**User Story:** As a Program_Manager, I want to create and manage subsidy programs and disbursements, so that eligible farmers receive financial support.

#### Acceptance Criteria

1. THE Frontend SHALL provide a subsidy program list page for Program_Managers and Government_Auditors showing all programs with their status (Draft, Active, Closed).
2. THE Frontend SHALL provide a program creation form collecting title, description, start date, end date, and budget amount, submitting to `POST /programs`.
3. WHEN a Program_Manager views a Draft program, THE Frontend SHALL display an "Activate" button that calls `PUT /programs/{id}/activate`.
4. WHEN a Program_Manager views an Active program, THE Frontend SHALL display a "Close" button that calls `PUT /programs/{id}/close` and a disbursement creation form.
5. THE Frontend SHALL display the program's total budget, total disbursed amount, and remaining budget on the program detail page.
6. WHEN a Program_Manager creates a disbursement, THE Frontend SHALL submit to `POST /programs/{id}/disbursements` and display the result, including a budget-exceeded error if applicable.
7. THE Frontend SHALL provide a disbursements list showing status (Pending, Approved, Disbursed, Failed) with an "Approve" button for Pending disbursements that calls `PUT /disbursements/{id}/approve`.

---

### Requirement 9: Compliance Records and Audit Management

**User Story:** As a Compliance_Officer or Government_Auditor, I want to manage compliance records and conduct audits, so that policy adherence is documented and systemic issues are identified.

#### Acceptance Criteria

1. THE Frontend SHALL provide a compliance record creation form for Compliance_Officers collecting entity type, entity ID, check result (Pass/Fail), date, and notes, submitting to `POST /compliance-records`.
2. THE Frontend SHALL provide a compliance record list view filterable by entity type and entity ID that calls `GET /compliance-records`.
3. THE Frontend SHALL provide an audit creation form for Compliance_Officers and Government_Auditors collecting audit scope, submitting to `POST /audits`.
4. WHEN an auditor views an In_Progress audit, THE Frontend SHALL display a findings input form with a "Submit Findings" button that calls `PUT /audits/{id}/findings`.
5. WHEN an audit status is Completed, THE Frontend SHALL display an "Export PDF" button that calls `GET /audits/{id}/export` and triggers a file download.
6. THE Frontend SHALL display a loading indicator while the PDF export is being generated and SHALL display an error message if the export takes longer than 10 seconds.

---

### Requirement 10: Reporting and Dashboard

**User Story:** As a Program_Manager, Market_Officer, or Government_Auditor, I want to view dashboards and generate reports, so that I can monitor system performance and program outcomes.

#### Acceptance Criteria

1. WHEN an authorized user navigates to the Dashboard, THE Frontend SHALL call `GET /dashboard` and display KPI cards for total active farmers, total crop listings, total transaction volume, and total subsidy disbursed.
2. THE Frontend SHALL display a loading skeleton while dashboard data is being fetched and SHALL display an error state with a retry button if the request fails.
3. THE Frontend SHALL provide a report generation form collecting scope and date range (up to 12 months), submitting to `POST /reports`.
4. WHEN a report is being generated, THE Frontend SHALL display a progress indicator and poll for completion status.
5. WHEN a report is ready, THE Frontend SHALL provide export buttons for CSV and PDF formats that call `GET /reports/{id}/export` and trigger file downloads.
6. THE Frontend SHALL display a reports history list showing previously generated reports with their scope, generation timestamp, and export options.

---

### Requirement 11: Notifications

**User Story:** As any registered user, I want to receive and manage in-app notifications, so that I am informed of events relevant to my role without manually polling the system.

#### Acceptance Criteria

1. THE Frontend SHALL display a Notification_Panel accessible from the main navigation that shows the count of unread notifications.
2. WHEN a user opens the Notification_Panel, THE Frontend SHALL call `GET /notifications/me` and display notifications paginated at 50 per page, showing message content, channel, status, and timestamp.
3. WHEN a user clicks an unread notification, THE Frontend SHALL call `PUT /notifications/{id}/read` and update the notification status to Read in the UI without a full page reload.
4. THE Frontend SHALL poll `GET /notifications/me` at 30-second intervals while the user is authenticated to update the unread notification count.
5. WHEN a new unread notification is detected during polling, THE Frontend SHALL display a brief toast message with the notification content.

---

### Requirement 12: User and Role Management (Administrator)

**User Story:** As an Administrator, I want to manage user accounts and role assignments, so that access control is maintained across the platform.

#### Acceptance Criteria

1. THE Frontend SHALL provide a user management list page for Administrators showing all users with their current role and status.
2. WHEN an Administrator selects a user, THE Frontend SHALL display a role assignment form with a dropdown of available roles that submits to `PUT /roles/{userId}`.
3. WHEN an Administrator deactivates a Farmer account, THE Frontend SHALL call `PUT /farmers/{id}/status` with status Inactive and display a confirmation.
4. THE Frontend SHALL provide an audit log viewer for Compliance_Officers and Government_Auditors that calls `GET /audit-log` with filter controls for date range, action type, and resource type, displaying results paginated at 50 per page.

---

### Requirement 13: Market Officer Listing Approval

**User Story:** As a Market_Officer, I want to approve or reject crop listings, so that only legitimate produce is visible to traders.

#### Acceptance Criteria

1. THE Frontend SHALL provide a listing approval queue for Market_Officers showing all CropListings with status Pending_Approval.
2. WHEN a Market_Officer views a pending listing, THE Frontend SHALL display listing details and provide "Approve" and "Reject" action buttons that call `PUT /listings/{id}`.
3. WHEN a Market_Officer clicks "Reject", THE Frontend SHALL require a rejection reason before submitting the request.
4. WHEN the backend confirms an approval or rejection, THE Frontend SHALL remove the listing from the pending queue and display a success message.

---

### Requirement 14: Global UI and Error Handling

**User Story:** As any user, I want the application to be responsive, accessible, and to handle errors gracefully, so that I can use the platform effectively on any device.

#### Acceptance Criteria

1. THE Frontend SHALL be responsive and usable on screen widths from 320px (mobile) to 1920px (desktop) without horizontal scrolling.
2. THE Frontend SHALL display a loading indicator for any API request that takes longer than 500ms to respond.
3. WHEN the backend returns a 400 Bad Request with field-level errors, THE Frontend SHALL display each error message adjacent to the corresponding form field.
4. WHEN the backend returns a 500 Internal Server Error, THE Frontend SHALL display a generic error message with a correlation ID if provided, and SHALL NOT expose stack traces or internal details.
5. WHEN the user's network connection is lost, THE Frontend SHALL display a connectivity warning banner and disable form submission until connectivity is restored.
6. THE Frontend SHALL support the latest two major versions of Chrome, Firefox, Safari, and Edge.
7. THE Frontend SHALL implement keyboard navigation and ARIA labels on all interactive elements to support screen reader accessibility.
