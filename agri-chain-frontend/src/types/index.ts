// ─── Auth ────────────────────────────────────────────────────────────────────
export type UserRole =
  | 'Farmer'
  | 'Trader'
  | 'Market_Officer'
  | 'Program_Manager'
  | 'Administrator'
  | 'Compliance_Officer'
  | 'Government_Auditor';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  role: UserRole;
  userId: string;
  expiresAt: string; // ISO timestamp
}

// ─── Farmer ──────────────────────────────────────────────────────────────────
export type FarmerStatus = 'Pending_Verification' | 'Active' | 'Inactive';

export interface FarmerProfile {
  id: string;
  userId: string;
  name: string;
  dateOfBirth: string;
  gender: string;
  address: string;
  contactInfo: string;
  landDetails: string;
  status: FarmerStatus;
}

export interface FarmerRegistrationRequest {
  name: string;
  dateOfBirth: string;
  gender: string;
  address: string;
  contactInfo: string;
  landDetails: string;
}

export type DocumentType = 'National_ID' | 'Land_Title' | 'Tax_Certificate';
export type VerificationStatus = 'Pending' | 'Verified' | 'Rejected';

export interface FarmerDocument {
  id: string;
  farmerId: string;
  documentType: DocumentType;
  verificationStatus: VerificationStatus;
  rejectionReason?: string;
  uploadedAt: string;
}

// ─── Crop ────────────────────────────────────────────────────────────────────
export type ListingStatus = 'Pending_Approval' | 'Active' | 'Rejected' | 'Closed';

export interface CropListing {
  id: string;
  farmerId: string;
  cropType: string;
  quantity: number;
  availableQuantity: number;
  pricePerUnit: number;
  location: string;
  status: ListingStatus;
  rejectionReason?: string;
  createdAt: string;
}

export interface CreateListingRequest {
  cropType: string;
  quantity: number;
  pricePerUnit: number;
  location: string;
}

export type OrderStatus = 'Pending' | 'Confirmed' | 'Declined' | 'Cancelled';

export interface Order {
  id: string;
  listingId: string;
  traderId: string;
  quantity: number;
  status: OrderStatus;
  createdAt: string;
}

// ─── Transaction ─────────────────────────────────────────────────────────────
export type TransactionStatus = 'Pending_Payment' | 'Settled' | 'Cancelled';

export interface Transaction {
  id: string;
  orderId: string;
  amount: number;
  status: TransactionStatus;
  createdAt: string;
  expiresAt: string;
}

export type PaymentMethod = 'Bank_Transfer' | 'Mobile_Money' | 'Card';
export type PaymentStatus = 'Processing' | 'Completed' | 'Failed';

export interface Payment {
  id: string;
  transactionId: string;
  method: PaymentMethod;
  status: PaymentStatus;
  failureReason?: string;
  createdAt: string;
}

export interface PaymentRequest {
  method: PaymentMethod;
  gatewayRef?: string;
}

// ─── Subsidy ─────────────────────────────────────────────────────────────────
export type SubsidyProgramStatus = 'Draft' | 'Active' | 'Closed';

export interface SubsidyProgram {
  id: string;
  title: string;
  description: string;
  startDate: string;
  endDate: string;
  budgetAmount: number;
  totalDisbursed: number;
  status: SubsidyProgramStatus;
  createdBy: string;
}

export interface CreateProgramRequest {
  title: string;
  description: string;
  startDate: string;
  endDate: string;
  budgetAmount: number;
}

export type DisbursementStatus = 'Pending' | 'Approved' | 'Disbursed' | 'Failed';

export interface Disbursement {
  id: string;
  programId: string;
  farmerId: string;
  amount: number;
  status: DisbursementStatus;
  approvedBy?: string;
  approvedAt?: string;
  programCycle: string;
}

export interface CreateDisbursementRequest {
  farmerId: string;
  amount: number;
  programCycle: string;
}

// ─── Compliance ──────────────────────────────────────────────────────────────
export type CheckResult = 'Pass' | 'Fail';

export interface ComplianceRecord {
  id: string;
  entityType: string;
  entityId: string;
  checkResult: CheckResult;
  checkDate: string;
  notes: string;
}

export interface CreateComplianceRecordRequest {
  entityType: string;
  entityId: string;
  checkResult: CheckResult;
  checkDate: string;
  notes: string;
}

export type AuditStatus = 'In_Progress' | 'Completed';

export interface Audit {
  id: string;
  scope: string;
  status: AuditStatus;
  findings?: string;
  initiatedBy: string;
  initiatedAt: string;
  completedAt?: string;
}

// ─── Notification ─────────────────────────────────────────────────────────────
export type NotificationChannel = 'In_App' | 'SMS' | 'Email';
export type NotificationStatus = 'Pending' | 'Delivered' | 'Read' | 'Failed';

export interface Notification {
  id: string;
  channel: NotificationChannel;
  message: string;
  status: NotificationStatus;
  createdAt: string;
  readAt?: string;
}

// ─── Dashboard ───────────────────────────────────────────────────────────────
export interface DashboardKPIs {
  activeFarmerCount: number;
  totalCropVolume: number;
  totalTransactionValue: number;
  totalSubsidiesDisbursed: number;
}

// ─── Reports ─────────────────────────────────────────────────────────────────
export interface ReportMetadata {
  id: string;
  scope: string;
  format: string;
  generatedBy: string;
  generationTimestamp: string;
}

export interface CreateReportRequest {
  scope: string;
  startDate: string;
  endDate: string;
  format: 'CSV' | 'PDF';
}

// ─── User (Admin) ─────────────────────────────────────────────────────────────
export type UserStatus = 'Active' | 'Locked' | 'Inactive';

export interface User {
  id: string;
  username: string;
  email: string;
  role: UserRole;
  status: UserStatus;
}

// ─── API Error ────────────────────────────────────────────────────────────────
export interface ApiFieldError {
  field: string;
  message: string;
}

export interface ApiErrorResponse {
  message: string;
  correlationId?: string;
  fields?: ApiFieldError[];
}

// ─── Pagination ───────────────────────────────────────────────────────────────
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// ─── Audit Log ────────────────────────────────────────────────────────────────
export interface AuditLog {
  id: string;
  userId: string;
  action: string;
  resourceType: string;
  resourceId: string;
  oldValue?: string;
  newValue?: string;
  timestamp: string;
}
