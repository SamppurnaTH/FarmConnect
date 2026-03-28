# Implementation Plan: Agri Chain Frontend

## Overview

Scaffold a React 18 + TypeScript SPA in `agri-chain-frontend/` alongside the existing `agri-chain/` backend. Tasks are ordered so each step builds on the previous: project setup → shared infrastructure → auth → role-based routing → feature pages → notifications → admin/compliance → global UX polish.

## Tasks

- [x] 1. Scaffold project and configure tooling
  - Run `npm create vite@latest agri-chain-frontend -- --template react-ts`
  - Install dependencies: `react-router-dom`, `zustand`, `axios`, `@hookform/resolvers`, `react-hook-form`, `zod`, `@radix-ui/react-*`, `tailwindcss`, `fast-check`, `vitest`, `@testing-library/react`, `@testing-library/user-event`, `jsdom`
  - Configure `vite.config.ts` with `test: { environment: 'jsdom', globals: true }`
  - Configure `tailwind.config.ts` and `postcss.config.js`
  - Add `VITE_API_BASE_URL` to `.env.example`
  - _Requirements: 14.1, 14.6_

- [x] 2. Define TypeScript types and API client
  - [x] 2.1 Create `src/types/index.ts` with all interfaces from the design (LoginRequest, LoginResponse, FarmerProfile, FarmerDocument, CropListing, Order, Transaction, Payment, SubsidyProgram, Disbursement, ComplianceRecord, Audit, Notification, DashboardKPIs, User, UserRole)
    - _Requirements: 1.1, 3.5, 5.4, 6.1, 7.1, 8.1, 9.2, 11.2, 12.1_

  - [x] 2.2 Create `src/api/client.ts` — Axios instance with request interceptor (attach Bearer token) and response interceptor (handle 401 → clear session + redirect, 403 → surface access-denied)
    - _Requirements: 1.7, 2.3, 1.5_

  - [x]* 2.3 Write property test for Axios request interceptor (Property 3)
    - **Property 3: Every authenticated request carries the Bearer token**
    - **Validates: Requirements 1.7**
    - Tag: `// Feature: agri-chain-frontend, Property 3: Every authenticated request carries the Bearer token`

  - [x] 2.4 Create per-service API modules: `src/api/auth.ts`, `src/api/farmers.ts`, `src/api/crops.ts`, `src/api/transactions.ts`, `src/api/subsidies.ts`, `src/api/compliance.ts`, `src/api/notifications.ts`, `src/api/users.ts`, `src/api/reports.ts`, `src/api/dashboard.ts`
    - Each module exports typed functions wrapping `apiClient` calls
    - _Requirements: 1.1, 3.1, 5.1, 6.1, 7.2, 8.2, 9.1, 11.2, 12.1, 10.1_

- [x] 3. Implement Auth Store and session management
  - [x] 3.1 Create `src/stores/authStore.ts` (Zustand) implementing `AuthState`: token (in-memory), role, userId, refreshTimer, login, logout, refresh actions
    - On login: store token, schedule refresh at `(expiry - 5min)`
    - On logout: call `POST /auth/logout`, clear token, cancel timer, redirect to `/login`
    - On refresh failure: clear token, redirect to `/login?reason=session_expired`
    - _Requirements: 1.1, 1.3, 1.4, 1.5_

  - [x]* 3.2 Write property test for login stores token and redirects (Property 1)
    - **Property 1: Successful login stores token and redirects**
    - **Validates: Requirements 1.1**
    - Tag: `// Feature: agri-chain-frontend, Property 1: Successful login stores token and redirects`

  - [x]* 3.3 Write property test for auth errors produce generic message (Property 2)
    - **Property 2: Authentication errors produce a generic, non-revealing message**
    - **Validates: Requirements 1.2**
    - Tag: `// Feature: agri-chain-frontend, Property 2: Authentication errors produce a generic, non-revealing message`

  - [x]* 3.4 Write property test for logout clears session (Property 4)
    - **Property 4: Logout clears the session**
    - **Validates: Requirements 1.3**
    - Tag: `// Feature: agri-chain-frontend, Property 4: Logout clears the session`

  - [x] 3.5 Write unit tests for auth store: login success, 423 locked response, session expiry redirect, refresh timer scheduling
    - _Requirements: 1.1, 1.2, 1.5, 1.6_

- [x] 4. Implement routing, RouteGuard, and permission matrix
  - [x] 4.1 Create `src/utils/permissions.ts` with `ROUTE_PERMISSIONS` matrix and `getPermittedRoutes(role)` helper
    - _Requirements: 2.1, 2.4_

  - [x] 4.2 Create `src/router/RouteGuard.tsx` — checks role against `ROUTE_PERMISSIONS`; redirects to `/403` if not permitted
    - _Requirements: 2.2_

  - [x] 4.3 Create `src/router/index.tsx` — define all routes wrapped in `<RouteGuard>`, including `/403` and `/login` public routes
    - _Requirements: 2.1, 2.2_

  - [x]* 4.4 Write property test for role-based rendering (Property 5)
    - **Property 5: Role-based rendering shows only permitted elements**
    - **Validates: Requirements 2.1, 2.4**
    - Tag: `// Feature: agri-chain-frontend, Property 5: Role-based rendering shows only permitted elements`

  - [x]* 4.5 Write property test for route guards redirect unauthorized navigation (Property 6)
    - **Property 6: Route guards redirect unauthorized navigation**
    - **Validates: Requirements 2.2**
    - Tag: `// Feature: agri-chain-frontend, Property 6: Route guards redirect unauthorized navigation`

  - [x]* 4.6 Write property test for 403 API responses (Property 7)
    - **Property 7: 403 API responses show access-denied without leaking data**
    - **Validates: Requirements 2.3**
    - Tag: `// Feature: agri-chain-frontend, Property 7: 403 API responses show access-denied without leaking data`

- [x] 5. Checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Build shared UI components
  - [x] 6.1 Create `src/components/LoadingSpinner.tsx` — visible after 500ms delay via `setTimeout` cancelled on unmount
    - _Requirements: 14.2_

  - [x] 6.2 Create `src/components/LoadingSkeleton.tsx` — KPI card placeholder skeletons
    - _Requirements: 10.2_

  - [x] 6.3 Create `src/components/ErrorBoundary.tsx` — catches render errors, shows generic message + correlation ID
    - _Requirements: 14.4_

  - [x] 6.4 Create `src/components/ConnectivityBanner.tsx` — listens to `offline`/`online` events, disables form submission while offline
    - _Requirements: 14.5_

  - [x] 6.5 Create `src/components/FieldError.tsx` — renders backend field-level validation errors adjacent to inputs
    - _Requirements: 3.2, 14.3_

  - [x] 6.6 Create `src/components/CountdownTimer.tsx` and `src/hooks/useCountdown.ts` — 1-second `setInterval`, fires callback at zero, clears on unmount
    - _Requirements: 7.3, 7.4_

  - [x] 6.7 Create `src/hooks/usePolling.ts` — `usePolling(fn, interval, active)` clears interval on unmount or when `active` is false
    - _Requirements: 7.5, 11.4_

  - [x]* 6.8 Write property test for field-level errors displayed adjacent to fields (Property 8)
    - **Property 8: Field-level errors are displayed adjacent to their fields**
    - **Validates: Requirements 3.2, 14.3**
    - Tag: `// Feature: agri-chain-frontend, Property 8: Field-level errors are displayed adjacent to their fields`

  - [x]* 6.9 Write property test for loading indicator after 500ms (Property 25)
    - **Property 25: Loading indicator appears after 500ms**
    - **Validates: Requirements 14.2**
    - Tag: `// Feature: agri-chain-frontend, Property 25: Loading indicator appears after 500ms`

  - [x]* 6.10 Write property test for 500 errors display generic message with correlation ID (Property 26)
    - **Property 26: 500 errors display generic message with correlation ID**
    - **Validates: Requirements 14.4**
    - Tag: `// Feature: agri-chain-frontend, Property 26: 500 errors display generic message with correlation ID`

  - [x]* 6.11 Write property test for countdown timer accuracy (Property 16)
    - **Property 16: Countdown timer displays correct remaining time**
    - **Validates: Requirements 7.3, 7.4**
    - Tag: `// Feature: agri-chain-frontend, Property 16: Countdown timer displays correct remaining time`

  - [x]* 6.12 Write property test for interactive elements have ARIA labels (Property 27)
    - **Property 27: Interactive elements have ARIA labels**
    - **Validates: Requirements 14.7**
    - Tag: `// Feature: agri-chain-frontend, Property 27: Interactive elements have ARIA labels`

- [x] 7. Implement Login page and Dashboard
  - [x] 7.1 Create `src/pages/LoginPage.tsx` — credential form using React Hook Form + Zod, calls `authStore.login`, displays generic error on failure, locked-account message on 423
    - _Requirements: 1.1, 1.2, 1.6_

  - [x] 7.2 Create `src/pages/DashboardPage.tsx` — calls `GET /dashboard`, renders four KPI cards, shows `<LoadingSkeleton>` during fetch, error state with retry button on failure
    - _Requirements: 10.1, 10.2_

  - [x]* 7.3 Write property test for dashboard KPI cards render all metrics (Property 28)
    - **Property 28: Dashboard KPI cards render all required metrics**
    - **Validates: Requirements 10.1**
    - Tag: `// Feature: agri-chain-frontend, Property 28: Dashboard KPI cards render all required metrics`

- [x] 8. Implement Farmer pages
  - [x] 8.1 Create `src/pages/farmer/FarmerRegistrationPage.tsx` — public form (name, DOB, gender, address, contact, land details), submits to `POST /farmers`, shows pending-verification confirmation, inline field errors, duplicate-contact error
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

  - [x] 8.2 Create `src/pages/farmer/FarmerProfilePage.tsx` — displays all profile fields, edit form submitting to `PUT /farmers/{id}`, contact re-verification notice, document upload control (`POST /farmers/{id}/documents`) with VerificationStatus badges
    - _Requirements: 3.5, 3.6, 3.7_

  - [x]* 8.3 Write property test for farmer profile renders all fields (Property 9)
    - **Property 9: Farmer profile renders all fields**
    - **Validates: Requirements 3.5**
    - Tag: `// Feature: agri-chain-frontend, Property 9: Farmer profile renders all fields`

  - [x]* 8.4 Write property test for document verification status badges match API data (Property 10)
    - **Property 10: Document verification status badges match API data**
    - **Validates: Requirements 3.7**
    - Tag: `// Feature: agri-chain-frontend, Property 10: Document verification status badges match API data`

  - [x] 8.5 Create `src/pages/farmer/MyListingsPage.tsx` — listing list with status, create form (`POST /listings`), pending orders per Active listing with Accept/Decline buttons calling `PUT /orders/{id}/accept` and `PUT /orders/{id}/decline`, edit form for quantity/price (`PUT /listings/{id}`)
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8_

  - [x] 8.6 Create `src/pages/farmer/FarmerOrdersPage.tsx` — order history list with current status
    - _Requirements: 5.6_

  - [x]* 8.7 Write property test for entity status updates reflected without page reload (Property 13)
    - **Property 13: Entity status updates are reflected in local state without page reload**
    - **Validates: Requirements 4.4, 5.7, 5.8, 13.4**
    - Tag: `// Feature: agri-chain-frontend, Property 13: Entity status updates are reflected in local state without page reload`

- [x] 9. Checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 10. Implement Market Officer pages
  - [x] 10.1 Create `src/pages/officer/FarmerManagementPage.tsx` — Pending_Verification queue, document viewer with Verify/Reject buttons (`PUT /farmers/{id}/documents/{docId}/verify`), rejection reason required, approve registration (`PUT /farmers/{id}/status`), status update reflected without reload
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [x]* 10.2 Write property test for status-filtered queues show only matching items (Property 11)
    - **Property 11: Status-filtered queues show only matching items**
    - **Validates: Requirements 4.1, 13.1**
    - Tag: `// Feature: agri-chain-frontend, Property 11: Status-filtered queues show only matching items`

  - [x]* 10.3 Write property test for rejection actions require a non-empty reason (Property 12)
    - **Property 12: Rejection actions require a non-empty reason**
    - **Validates: Requirements 4.3, 13.3**
    - Tag: `// Feature: agri-chain-frontend, Property 12: Rejection actions require a non-empty reason`

  - [x] 10.4 Create `src/pages/officer/ListingApprovalPage.tsx` — Pending_Approval queue, listing details, Approve/Reject buttons (`PUT /listings/{id}`), rejection reason required, remove from queue on confirmation
    - _Requirements: 13.1, 13.2, 13.3, 13.4_

- [x] 11. Implement Trader pages
  - [x] 11.1 Create `src/pages/trader/BrowseListingsPage.tsx` — paginated listing grid calling `GET /listings`, filter controls (crop type, location, price range) updating query params without reload, order placement form (`POST /listings/{id}/orders`), insufficient-quantity error handling
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

  - [x]* 11.2 Write property test for listing filter controls produce matching query parameters (Property 14)
    - **Property 14: Listing filter controls produce matching query parameters**
    - **Validates: Requirements 6.2**
    - Tag: `// Feature: agri-chain-frontend, Property 14: Listing filter controls produce matching query parameters`

  - [x] 11.3 Create `src/pages/trader/TraderOrdersPage.tsx` — order history with current status
    - _Requirements: 6.6_

- [x] 12. Implement Transactions page
  - [x] 12.1 Create `src/pages/shared/TransactionsPage.tsx` — transaction list (`GET /transactions`) showing status, amount, linked order; payment form for Pending_Payment (method selection, `POST /transactions/{id}/payments`); `<CountdownTimer>` per transaction; payment polling via `usePolling` at 10s; failure reason display
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_

  - [x]* 12.2 Write property test for transaction list renders all required fields (Property 15)
    - **Property 15: Transaction list renders all required fields**
    - **Validates: Requirements 7.1**
    - Tag: `// Feature: agri-chain-frontend, Property 15: Transaction list renders all required fields`

  - [x]* 12.3 Write property test for payment polling stops on terminal status (Property 17)
    - **Property 17: Payment polling stops on terminal status**
    - **Validates: Requirements 7.5**
    - Tag: `// Feature: agri-chain-frontend, Property 17: Payment polling stops on terminal status`

- [x] 13. Implement Subsidy and Disbursement pages
  - [x] 13.1 Create `src/pages/program/SubsidyProgramsPage.tsx` — program list with status, creation form (`POST /programs`), Activate button for Draft (`PUT /programs/{id}/activate`), Close button for Active (`PUT /programs/{id}/close`), budget/disbursed/remaining display, disbursement creation form (`POST /programs/{id}/disbursements`) with budget-exceeded error
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6_

  - [x] 13.2 Create `src/pages/program/DisbursementsPage.tsx` — disbursement list with status, Approve button for Pending (`PUT /disbursements/{id}/approve`)
    - _Requirements: 8.7_

  - [x]* 13.3 Write property test for status-driven action buttons match entity state (Property 18)
    - **Property 18: Status-driven action buttons match entity state**
    - **Validates: Requirements 8.3, 8.4, 8.7, 9.4, 9.5**
    - Tag: `// Feature: agri-chain-frontend, Property 18: Status-driven action buttons match entity state`

  - [x]* 13.4 Write property test for remaining budget computed correctly (Property 19)
    - **Property 19: Remaining budget is computed correctly**
    - **Validates: Requirements 8.5**
    - Tag: `// Feature: agri-chain-frontend, Property 19: Remaining budget is computed correctly`

- [x] 14. Checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 15. Implement Compliance and Audit pages
  - [x] 15.1 Create `src/pages/compliance/ComplianceRecordsPage.tsx` — record list filterable by entity type/ID (`GET /compliance-records`), creation form for Compliance_Officers (`POST /compliance-records`)
    - _Requirements: 9.1, 9.2_

  - [x] 15.2 Create `src/pages/compliance/AuditsPage.tsx` — audit list, creation form (`POST /audits`), findings form for In_Progress audits (`PUT /audits/{id}/findings`), Export PDF button for Completed audits (`GET /audits/{id}/export`) with 10-second AbortController timeout and loading indicator
    - _Requirements: 9.3, 9.4, 9.5, 9.6_

  - [x] 15.3 Create `src/pages/compliance/AuditLogPage.tsx` — paginated audit log (`GET /audit-log`) with date range, action type, and resource type filter controls, 50 per page
    - _Requirements: 12.4_

- [x] 16. Implement Notifications store and panel
  - [x] 16.1 Create `src/stores/notificationStore.ts` (Zustand) — notifications array, unreadCount, fetchNotifications, markRead actions
    - _Requirements: 11.1, 11.3_

  - [x] 16.2 Create `src/components/NotificationPanel.tsx` — bell icon with unread count badge, dropdown list (50/page), click-to-mark-read, polls via `usePolling` at 30s, triggers toast on new unread items
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_

  - [x] 16.3 Create `src/pages/shared/NotificationsPage.tsx` — full paginated notification history
    - _Requirements: 11.2_

  - [x]* 16.4 Write property test for unread notification count matches notification data (Property 20)
    - **Property 20: Unread notification count matches notification data**
    - **Validates: Requirements 11.1**
    - Tag: `// Feature: agri-chain-frontend, Property 20: Unread notification count matches notification data`

  - [x]* 16.5 Write property test for marking a notification read updates local state (Property 21)
    - **Property 21: Marking a notification read updates local state**
    - **Validates: Requirements 11.3**
    - Tag: `// Feature: agri-chain-frontend, Property 21: Marking a notification read updates local state`

  - [x]* 16.6 Write property test for notification polling fires at 30-second intervals (Property 22)
    - **Property 22: Notification polling fires at 30-second intervals**
    - **Validates: Requirements 11.4**
    - Tag: `// Feature: agri-chain-frontend, Property 22: Notification polling fires at 30-second intervals`

  - [x]* 16.7 Write property test for new unread notifications trigger a toast (Property 23)
    - **Property 23: New unread notifications trigger a toast**
    - **Validates: Requirements 11.5**
    - Tag: `// Feature: agri-chain-frontend, Property 23: New unread notifications trigger a toast`

- [x] 17. Implement Admin and Reports pages
  - [x] 17.1 Create `src/pages/admin/UserManagementPage.tsx` — user list with role and status, role assignment dropdown (`PUT /roles/{userId}`), deactivate action (`PUT /farmers/{id}/status` with Inactive)
    - _Requirements: 12.1, 12.2, 12.3_

  - [x]* 17.2 Write property test for user list renders role and status for every user (Property 24)
    - **Property 24: User list renders role and status for every user**
    - **Validates: Requirements 12.1**
    - Tag: `// Feature: agri-chain-frontend, Property 24: User list renders role and status for every user`

  - [x] 17.3 Create `src/pages/reports/ReportsPage.tsx` — report generation form (scope, date range up to 12 months, `POST /reports`), progress indicator with polling for completion, CSV/PDF export buttons (`GET /reports/{id}/export`), report history list
    - _Requirements: 10.3, 10.4, 10.5, 10.6_

  - [x]* 17.4 Write property test for report polling stops on terminal status (Property 29)
    - **Property 29: Report polling stops on terminal status**
    - **Validates: Requirements 10.4**
    - Tag: `// Feature: agri-chain-frontend, Property 29: Report polling stops on terminal status`

- [x] 18. Wire app shell and navigation
  - [x] 18.1 Create `src/App.tsx` — wrap router with `<ErrorBoundary>`, `<ConnectivityBanner>`, `<ToastProvider>`, and `<NotificationPanel>` in the authenticated layout
    - _Requirements: 14.4, 14.5, 11.5_

  - [x] 18.2 Create `src/components/NavMenu.tsx` — renders navigation links filtered by `getPermittedRoutes(role)`, keyboard-navigable, ARIA labels on all links
    - _Requirements: 2.1, 14.7_

  - [x] 18.3 Create `src/components/ToastProvider.tsx` — global toast context consumed by notification polling and action confirmations
    - _Requirements: 11.5_

- [x] 19. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Each task references specific requirements for traceability
- All 29 correctness properties from the design document are covered by exactly one property-based test sub-task each
- Property tests use `fast-check` with `numRuns: 100` and the tag format `// Feature: agri-chain-frontend, Property {N}: {property_text}`
- Unit tests cover specific examples, edge cases, and integration points not already covered by property tests
