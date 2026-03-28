import type { UserRole } from '../types';

// ─── Route Permission Matrix ──────────────────────────────────────────────────
// Source of truth for all route access decisions (mirrors design doc)
export const ROUTE_PERMISSIONS: Record<string, UserRole[]> = {
  '/dashboard':        ['Farmer','Trader','Market_Officer','Program_Manager','Administrator','Compliance_Officer','Government_Auditor'],
  '/profile':          ['Farmer','Trader'],
  '/register':         ['Farmer'], // public reg page is also listed here for nav
  '/farmers':          ['Market_Officer','Administrator'],
  '/listings/mine':    ['Farmer'],
  '/listings/browse':  ['Trader','Market_Officer'],
  '/listings/approve': ['Market_Officer'],
  '/orders/farmer':    ['Farmer'],
  '/orders/trader':    ['Trader'],
  '/transactions':     ['Farmer','Trader','Market_Officer'],
  '/subsidies':        ['Program_Manager','Government_Auditor'],
  '/disbursements':    ['Program_Manager','Government_Auditor'],
  '/compliance':       ['Compliance_Officer'],
  '/audits':           ['Compliance_Officer','Government_Auditor'],
  '/reports':          ['Market_Officer','Program_Manager','Government_Auditor'],
  '/users':            ['Administrator'],
  '/audit-log':        ['Compliance_Officer','Government_Auditor'],
  '/notifications':    ['Farmer','Trader','Market_Officer','Program_Manager','Administrator','Compliance_Officer','Government_Auditor'],
};

// ─── Navigation Labels ────────────────────────────────────────────────────────
export const ROUTE_LABELS: Record<string, string> = {
  '/dashboard':        'Dashboard',
  '/profile':          'My Profile',
  '/farmers':          'Farmer Management',
  '/listings/mine':    'My Listings',
  '/listings/browse':  'Browse Listings',
  '/listings/approve': 'Listing Approvals',
  '/orders/farmer':    'My Orders',
  '/orders/trader':    'My Orders',
  '/transactions':     'Transactions',
  '/subsidies':        'Subsidy Programs',
  '/disbursements':    'Disbursements',
  '/compliance':       'Compliance Records',
  '/audits':           'Audits',
  '/reports':          'Reports',
  '/users':            'User Management',
  '/audit-log':        'Audit Log',
  '/notifications':    'Notifications',
};

// ─── Helpers ──────────────────────────────────────────────────────────────────
export function getPermittedRoutes(role: UserRole): string[] {
  return Object.entries(ROUTE_PERMISSIONS)
    .filter(([, roles]) => roles.includes(role))
    .map(([route]) => route);
}

export function isRoutePermitted(route: string, role: UserRole | null): boolean {
  if (!role) return false;
  const permitted = ROUTE_PERMISSIONS[route];
  return permitted ? permitted.includes(role) : false;
}

export const ALL_ROLES: UserRole[] = [
  'Farmer', 'Trader', 'Market_Officer', 'Program_Manager',
  'Administrator', 'Compliance_Officer', 'Government_Auditor',
];
