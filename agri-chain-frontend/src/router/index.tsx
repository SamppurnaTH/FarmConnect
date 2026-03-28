import React, { Suspense, lazy } from 'react';
import { createBrowserRouter, RouterProvider, Navigate } from 'react-router-dom';
import { RouteGuard } from './RouteGuard';
import { LoadingSpinner } from '../components/LoadingSpinner';

// ─── Lazy-loaded pages ────────────────────────────────────────────────────────
const LoginPage                = lazy(() => import('../pages/LoginPage'));
const FarmerRegistrationPage   = lazy(() => import('../pages/farmer/FarmerRegistrationPage'));
const DashboardPage            = lazy(() => import('../pages/DashboardPage'));
const FarmerProfilePage        = lazy(() => import('../pages/farmer/FarmerProfilePage'));
const MyListingsPage           = lazy(() => import('../pages/farmer/MyListingsPage'));
const FarmerOrdersPage         = lazy(() => import('../pages/farmer/FarmerOrdersPage'));
const BrowseListingsPage       = lazy(() => import('../pages/trader/BrowseListingsPage'));
const TraderOrdersPage         = lazy(() => import('../pages/trader/TraderOrdersPage'));
const TransactionsPage         = lazy(() => import('../pages/shared/TransactionsPage'));
const FarmerManagementPage     = lazy(() => import('../pages/officer/FarmerManagementPage'));
const ListingApprovalPage      = lazy(() => import('../pages/officer/ListingApprovalPage'));
const SubsidyProgramsPage      = lazy(() => import('../pages/program/SubsidyProgramsPage'));
const DisbursementsPage        = lazy(() => import('../pages/program/DisbursementsPage'));
const UserManagementPage       = lazy(() => import('../pages/admin/UserManagementPage'));
const ComplianceRecordsPage    = lazy(() => import('../pages/compliance/ComplianceRecordsPage'));
const AuditsPage               = lazy(() => import('../pages/compliance/AuditsPage'));
const AuditLogPage             = lazy(() => import('../pages/compliance/AuditLogPage'));
const ReportsPage              = lazy(() => import('../pages/reports/ReportsPage'));
const NotificationsPage        = lazy(() => import('../pages/shared/NotificationsPage'));
const ForbiddenPage            = lazy(() => import('../pages/ForbiddenPage'));
const NotFoundPage             = lazy(() => import('../pages/NotFoundPage'));

// ─── Guarded route helper ─────────────────────────────────────────────────────
function G({ path, element }: { path: string; element: React.ReactNode }) {
  return <RouteGuard route={path}>{element}</RouteGuard>;
}

const router = createBrowserRouter([
  // Public
  { path: '/login',    element: <Suspense fallback={<LoadingSpinner />}><LoginPage /></Suspense> },
  { path: '/register', element: <Suspense fallback={<LoadingSpinner />}><FarmerRegistrationPage /></Suspense> },
  { path: '/403',      element: <Suspense fallback={<LoadingSpinner />}><ForbiddenPage /></Suspense> },
  { path: '/404',      element: <Suspense fallback={<LoadingSpinner />}><NotFoundPage /></Suspense> },

  // Authenticated + role-guarded
  { path: '/dashboard',        element: <Suspense fallback={<LoadingSpinner />}><G path="/dashboard"        element={<DashboardPage />} /></Suspense> },
  { path: '/profile',          element: <Suspense fallback={<LoadingSpinner />}><G path="/profile"          element={<FarmerProfilePage />} /></Suspense> },
  { path: '/farmers',          element: <Suspense fallback={<LoadingSpinner />}><G path="/farmers"          element={<FarmerManagementPage />} /></Suspense> },
  { path: '/listings/mine',    element: <Suspense fallback={<LoadingSpinner />}><G path="/listings/mine"    element={<MyListingsPage />} /></Suspense> },
  { path: '/listings/browse',  element: <Suspense fallback={<LoadingSpinner />}><G path="/listings/browse"  element={<BrowseListingsPage />} /></Suspense> },
  { path: '/listings/approve', element: <Suspense fallback={<LoadingSpinner />}><G path="/listings/approve" element={<ListingApprovalPage />} /></Suspense> },
  { path: '/orders/farmer',    element: <Suspense fallback={<LoadingSpinner />}><G path="/orders/farmer"    element={<FarmerOrdersPage />} /></Suspense> },
  { path: '/orders/trader',    element: <Suspense fallback={<LoadingSpinner />}><G path="/orders/trader"    element={<TraderOrdersPage />} /></Suspense> },
  { path: '/transactions',     element: <Suspense fallback={<LoadingSpinner />}><G path="/transactions"     element={<TransactionsPage />} /></Suspense> },
  { path: '/subsidies',        element: <Suspense fallback={<LoadingSpinner />}><G path="/subsidies"        element={<SubsidyProgramsPage />} /></Suspense> },
  { path: '/disbursements',    element: <Suspense fallback={<LoadingSpinner />}><G path="/disbursements"    element={<DisbursementsPage />} /></Suspense> },
  { path: '/compliance',       element: <Suspense fallback={<LoadingSpinner />}><G path="/compliance"       element={<ComplianceRecordsPage />} /></Suspense> },
  { path: '/audits',           element: <Suspense fallback={<LoadingSpinner />}><G path="/audits"           element={<AuditsPage />} /></Suspense> },
  { path: '/reports',          element: <Suspense fallback={<LoadingSpinner />}><G path="/reports"          element={<ReportsPage />} /></Suspense> },
  { path: '/users',            element: <Suspense fallback={<LoadingSpinner />}><G path="/users"            element={<UserManagementPage />} /></Suspense> },
  { path: '/audit-log',        element: <Suspense fallback={<LoadingSpinner />}><G path="/audit-log"        element={<AuditLogPage />} /></Suspense> },
  { path: '/notifications',    element: <Suspense fallback={<LoadingSpinner />}><G path="/notifications"    element={<NotificationsPage />} /></Suspense> },

  // Catch-all
  { path: '/',  element: <Navigate to="/dashboard" replace /> },
  { path: '*',  element: <Suspense fallback={<LoadingSpinner />}><NotFoundPage /></Suspense> },
]);

export const AppRouter: React.FC = () => <RouterProvider router={router} />;
