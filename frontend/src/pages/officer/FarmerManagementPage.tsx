import React, { useEffect, useState, useCallback, useRef } from 'react';
import { NavMenu } from '../../components/NavMenu';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { farmersApi } from '../../api/farmers';
import { cropsApi } from '../../api/crops';
import { useToast } from '../../components/ToastProvider';
import { useIsOnline } from '../../components/ConnectivityBanner';
import type {
  FarmerProfile, FarmerDocument, FarmerStatus,
  VerificationStatus, CropListing,
} from '../../types';
import {
  ChevronDown, ChevronUp, Check, X, ExternalLink,
  AlertTriangle, Search, ChevronLeft, ChevronRight,
  ListChecks, Sprout,
} from 'lucide-react';

type StatusFilter = 'Pending_Verification' | 'Active' | 'Inactive';

const STATUS_TABS: { label: string; value: StatusFilter }[] = [
  { label: 'Pending Verification', value: 'Pending_Verification' },
  { label: 'Active',               value: 'Active' },
  { label: 'Inactive',             value: 'Inactive' },
];

const PAGE_SIZE = 15;

/**
 * FarmerManagementPage — Requirement 4.1-4.5
 * Market Officer and Administrator view.
 *
 * Features:
 * - Tab filter by farmer status (Pending / Active / Inactive)
 * - Search farmers by name (debounced, post-decryption on backend)
 * - Pagination (15 per page)
 * - Expand farmer to view profile details + documents + listings
 * - View uploaded document files
 * - Verify / reject individual documents
 * - Bulk verify all pending documents for a farmer
 * - Activate / deactivate farmer (Inactive ↔ Active ↔ Pending)
 * - Guard: cannot activate without at least one verified document
 */
const FarmerManagementPage: React.FC = () => {
  const { showToast } = useToast();
  const isOnline = useIsOnline();

  const [activeTab, setActiveTab]     = useState<StatusFilter>('Pending_Verification');
  const [search, setSearch]           = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [page, setPage]               = useState(0);
  const [totalPages, setTotalPages]   = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const [farmers, setFarmers]         = useState<FarmerProfile[]>([]);
  const [docs, setDocs]               = useState<Record<string, FarmerDocument[]>>({});
  const [listings, setListings]       = useState<Record<string, CropListing[]>>({});
  const [expandedId, setExpandedId]   = useState<string | null>(null);
  const [activeSection, setActiveSection] = useState<Record<string, 'docs' | 'listings'>>({});
  const [loading, setLoading]         = useState(true);
  const [rejectReason, setRejectReason]   = useState<Record<string, string>>({});
  const [rejectError, setRejectError]     = useState<Record<string, string>>({});
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  // ── Debounce search input ─────────────────────────────────────────────────
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const handleSearchChange = (value: string) => {
    setSearch(value);
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setDebouncedSearch(value);
      setPage(0);
    }, 350);
  };

  // ── Load farmers ──────────────────────────────────────────────────────────
  const loadFarmers = useCallback(async (
    status: StatusFilter, searchTerm: string, pageNum: number
  ) => {
    setLoading(true);
    setExpandedId(null);
    try {
      const data = await farmersApi.listPaged({
        status,
        search: searchTerm || undefined,
        page: pageNum,
        size: PAGE_SIZE,
      });
      setFarmers(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch {
      showToast('Failed to load farmers', 'error');
    } finally {
      setLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    loadFarmers(activeTab, debouncedSearch, page);
  }, [activeTab, debouncedSearch, page, loadFarmers]);

  // Reset page when tab changes
  const handleTabChange = (tab: StatusFilter) => {
    setActiveTab(tab);
    setPage(0);
    setSearch('');
    setDebouncedSearch('');
  };

  // ── Expand / collapse farmer row ──────────────────────────────────────────
  const toggle = async (id: string) => {
    const next = expandedId === id ? null : id;
    setExpandedId(next);
    if (next) {
      // Default to docs section
      setActiveSection((p) => ({ ...p, [next]: p[next] ?? 'docs' }));
      if (!docs[next]) {
        try {
          const d = await farmersApi.getDocuments(next);
          setDocs((p) => ({ ...p, [next]: d }));
        } catch {
          showToast('Failed to load documents', 'error');
        }
      }
    }
  };

  // ── Load farmer's listings ────────────────────────────────────────────────
  const loadListings = async (farmerId: string) => {
    if (listings[farmerId]) return; // already loaded
    try {
      const data = await cropsApi.getMyListings(farmerId);
      setListings((p) => ({ ...p, [farmerId]: data }));
    } catch {
      showToast('Failed to load listings', 'error');
    }
  };

  const handleSectionChange = (farmerId: string, section: 'docs' | 'listings') => {
    setActiveSection((p) => ({ ...p, [farmerId]: section }));
    if (section === 'listings') loadListings(farmerId);
  };

  // ── Document verification ─────────────────────────────────────────────────
  const handleVerify = async (
    farmerId: string, docId: string, status: VerificationStatus
  ) => {
    const reason = rejectReason[docId];
    if (status === 'Rejected' && !reason?.trim()) {
      setRejectError((p) => ({ ...p, [docId]: 'Rejection reason is required.' }));
      return;
    }
    setRejectError((p) => ({ ...p, [docId]: '' }));
    setActionLoading(docId);
    try {
      await farmersApi.verifyDocument(farmerId, docId, status, reason);
      const updated = await farmersApi.getDocuments(farmerId);
      setDocs((p) => ({ ...p, [farmerId]: updated }));
      showToast(`Document ${status === 'Verified' ? 'verified' : 'rejected'}`, 'success');
    } catch {
      showToast('Action failed. Please try again.', 'error');
    } finally {
      setActionLoading(null);
    }
  };

  // ── Bulk verify all pending documents ────────────────────────────────────
  const handleBulkVerify = async (farmerId: string) => {
    const pendingDocs = (docs[farmerId] ?? []).filter(
      (d) => d.verificationStatus === 'Pending'
    );
    if (pendingDocs.length === 0) return;

    setActionLoading(`bulk-${farmerId}`);
    try {
      await Promise.all(
        pendingDocs.map((d) =>
          farmersApi.verifyDocument(farmerId, d.id, 'Verified')
        )
      );
      const updated = await farmersApi.getDocuments(farmerId);
      setDocs((p) => ({ ...p, [farmerId]: updated }));
      showToast(`${pendingDocs.length} document(s) verified`, 'success');
    } catch {
      showToast('Bulk verify failed. Please try individually.', 'error');
    } finally {
      setActionLoading(null);
    }
  };

  // ── Farmer status change ──────────────────────────────────────────────────
  const handleStatusChange = async (farmerId: string, newStatus: FarmerStatus) => {
    if (newStatus === 'Active') {
      const farmerDocs = docs[farmerId] ?? [];
      const hasVerified = farmerDocs.some((d) => d.verificationStatus === 'Verified');
      if (!hasVerified) {
        showToast('Cannot activate — at least one document must be verified first.', 'error');
        return;
      }
    }

    setActionLoading(farmerId);
    try {
      await farmersApi.updateStatus(farmerId, newStatus);
      setFarmers((prev) => prev.filter((f) => f.id !== farmerId));
      setTotalElements((n) => n - 1);
      const label = newStatus === 'Active' ? 'activated' : 'deactivated';
      showToast(`Farmer ${label} successfully`, 'success');
    } catch {
      showToast('Status update failed. Please try again.', 'error');
    } finally {
      setActionLoading(null);
    }
  };

  // ── Helpers ───────────────────────────────────────────────────────────────
  const hasVerifiedDoc = (farmerId: string) =>
    (docs[farmerId] ?? []).some((d) => d.verificationStatus === 'Verified');

  const pendingDocCount = (farmerId: string) =>
    (docs[farmerId] ?? []).filter((d) => d.verificationStatus === 'Pending').length;

  const listingStatusClass: Record<string, string> = {
    Pending_Approval: 'badge-pending',
    Active:           'badge-active',
    Rejected:         'badge-rejected',
    Closed:           'badge-closed',
  };

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <div className="min-h-screen bg-gray-50">
      <NavMenu />
      <main className="max-w-5xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">Farmer Management</h1>

        {/* ── Status tabs ── */}
        <div className="flex gap-2 mb-4 border-b border-gray-200">
          {STATUS_TABS.map((tab) => (
            <button
              key={tab.value}
              onClick={() => handleTabChange(tab.value)}
              className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
                activeTab === tab.value
                  ? 'border-primary-600 text-primary-700'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
              aria-selected={activeTab === tab.value}
              role="tab"
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* ── Search bar ── */}
        <div className="relative mb-4">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" aria-hidden="true" />
          <input
            type="search"
            className="input-field pl-9"
            placeholder="Search by farmer name…"
            value={search}
            onChange={(e) => handleSearchChange(e.target.value)}
            aria-label="Search farmers by name"
          />
        </div>

        {loading ? (
          <LoadingSpinner />
        ) : farmers.length === 0 ? (
          <div className="card text-center py-12 text-gray-500">
            {debouncedSearch
              ? `No farmers matching "${debouncedSearch}" with status "${activeTab.replace(/_/g, ' ')}".`
              : `No farmers with status "${activeTab.replace(/_/g, ' ')}".`}
          </div>
        ) : (
          <>
            <p className="text-sm text-gray-500 mb-3">
              {totalElements} farmer{totalElements !== 1 ? 's' : ''}
              {debouncedSearch && ` matching "${debouncedSearch}"`}
            </p>

            <div className="space-y-3">
              {farmers.map((f) => {
                const farmerDocs  = docs[f.id] ?? [];
                const isExpanded  = expandedId === f.id;
                const docsLoaded  = !!docs[f.id];
                const section     = activeSection[f.id] ?? 'docs';
                const pendingCount = pendingDocCount(f.id);

                return (
                  <div key={f.id} className="bg-white rounded-xl border border-gray-200 overflow-hidden">

                    {/* ── Header row ── */}
                    <button
                      onClick={() => toggle(f.id)}
                      className="w-full flex items-center justify-between px-6 py-4 text-left hover:bg-gray-50 transition-colors"
                      aria-expanded={isExpanded}
                      aria-label={`Toggle details for ${f.name}`}
                    >
                      <div>
                        <p className="font-semibold text-gray-900">{f.name}</p>
                        <p className="text-sm text-gray-500 mt-0.5">{f.contactInfo}</p>
                        <p className="text-xs text-gray-400 font-mono mt-0.5">{f.id}</p>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className={
                          f.status === 'Active'   ? 'badge-active' :
                          f.status === 'Inactive' ? 'badge-closed' : 'badge-pending'
                        }>
                          {f.status.replace(/_/g, ' ')}
                        </span>
                        {isExpanded
                          ? <ChevronUp className="w-4 h-4 text-gray-400" aria-hidden="true" />
                          : <ChevronDown className="w-4 h-4 text-gray-400" aria-hidden="true" />}
                      </div>
                    </button>

                    {/* ── Expanded details ── */}
                    {isExpanded && (
                      <div className="border-t border-gray-100">

                        {/* Profile details */}
                        <div className="px-6 pt-4 pb-3 grid grid-cols-2 gap-x-6 gap-y-2 text-sm bg-gray-50">
                          {([
                            ['Gender',        f.gender],
                            ['Date of Birth', f.dateOfBirth],
                            ['Address',       f.address],
                            ['Land Details',  f.landDetails],
                          ] as [string, string | undefined][]).map(([label, value]) => (
                            <div key={label}>
                              <span className="text-gray-500">{label}: </span>
                              <span className="font-medium text-gray-900">{value || '—'}</span>
                            </div>
                          ))}
                        </div>

                        {/* Section tabs: Documents | Listings */}
                        <div className="flex gap-4 px-6 pt-3 border-b border-gray-100">
                          <button
                            onClick={() => handleSectionChange(f.id, 'docs')}
                            className={`flex items-center gap-1.5 pb-2 text-sm font-medium border-b-2 transition-colors ${
                              section === 'docs'
                                ? 'border-primary-600 text-primary-700'
                                : 'border-transparent text-gray-500 hover:text-gray-700'
                            }`}
                          >
                            <ListChecks className="w-4 h-4" aria-hidden="true" />
                            Documents
                            {docsLoaded && pendingCount > 0 && (
                              <span className="ml-1 px-1.5 py-0.5 text-xs bg-amber-100 text-amber-700 rounded-full">
                                {pendingCount} pending
                              </span>
                            )}
                          </button>
                          <button
                            onClick={() => handleSectionChange(f.id, 'listings')}
                            className={`flex items-center gap-1.5 pb-2 text-sm font-medium border-b-2 transition-colors ${
                              section === 'listings'
                                ? 'border-primary-600 text-primary-700'
                                : 'border-transparent text-gray-500 hover:text-gray-700'
                            }`}
                          >
                            <Sprout className="w-4 h-4" aria-hidden="true" />
                            Crop Listings
                          </button>
                        </div>

                        <div className="px-6 py-4">

                          {/* ── Documents section ── */}
                          {section === 'docs' && (
                            <>
                              {/* Bulk verify button */}
                              {docsLoaded && pendingCount > 0 && (
                                <div className="flex items-center justify-between mb-3">
                                  <p className="text-sm text-amber-700">
                                    {pendingCount} document{pendingCount !== 1 ? 's' : ''} pending review
                                  </p>
                                  <button
                                    onClick={() => handleBulkVerify(f.id)}
                                    className="btn-secondary text-xs flex items-center gap-1 disabled:opacity-50"
                                    disabled={!isOnline || actionLoading === `bulk-${f.id}`}
                                    aria-label="Verify all pending documents"
                                  >
                                    <Check className="w-3 h-3" aria-hidden="true" />
                                    {actionLoading === `bulk-${f.id}` ? 'Verifying…' : 'Verify All Pending'}
                                  </button>
                                </div>
                              )}

                              {!docsLoaded ? (
                                <p className="text-sm text-gray-400">Loading documents…</p>
                              ) : farmerDocs.length === 0 ? (
                                <div className="flex items-center gap-2 text-sm text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-4 py-3">
                                  <AlertTriangle className="w-4 h-4 shrink-0" aria-hidden="true" />
                                  No documents uploaded yet.
                                </div>
                              ) : (
                                <div className="space-y-3">
                                  {farmerDocs.map((doc) => (
                                    <div
                                      key={doc.id}
                                      className="flex items-start justify-between bg-gray-50 rounded-lg px-4 py-3 gap-4"
                                    >
                                      <div className="flex-1 min-w-0">
                                        <div className="flex items-center gap-2 flex-wrap">
                                          <p className="font-medium text-sm text-gray-900">
                                            {doc.documentType.replace(/_/g, ' ')}
                                          </p>
                                          <span className={
                                            doc.verificationStatus === 'Verified' ? 'badge-verified' :
                                            doc.verificationStatus === 'Rejected'  ? 'badge-rejected' : 'badge-pending'
                                          }>
                                            {doc.verificationStatus}
                                          </span>
                                          <a
                                            href={farmersApi.getDocumentFileUrl(f.id, doc.id)}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                            className="inline-flex items-center gap-1 text-xs text-primary-600 hover:underline"
                                            aria-label={`View ${doc.documentType.replace(/_/g, ' ')} file`}
                                          >
                                            <ExternalLink className="w-3 h-3" aria-hidden="true" />
                                            View file
                                          </a>
                                        </div>
                                        {doc.rejectionReason && (
                                          <p className="text-xs text-red-600 mt-1">
                                            Reason: {doc.rejectionReason}
                                          </p>
                                        )}
                                        <p className="text-xs text-gray-400 mt-1">
                                          Uploaded {new Date(doc.uploadedAt).toLocaleDateString()}
                                          {doc.reviewedAt && ` · Reviewed ${new Date(doc.reviewedAt).toLocaleDateString()}`}
                                        </p>
                                      </div>

                                      {doc.verificationStatus === 'Pending' && (
                                        <div className="flex items-center gap-2 shrink-0">
                                          <div className="flex flex-col gap-1">
                                            <input
                                              className="input-field text-xs w-44 py-1"
                                              placeholder="Rejection reason"
                                              aria-label={`Rejection reason for ${doc.documentType}`}
                                              value={rejectReason[doc.id] ?? ''}
                                              onChange={(e) => {
                                                setRejectReason((p) => ({ ...p, [doc.id]: e.target.value }));
                                                setRejectError((p) => ({ ...p, [doc.id]: '' }));
                                              }}
                                            />
                                            {rejectError[doc.id] && (
                                              <p className="text-xs text-red-500">{rejectError[doc.id]}</p>
                                            )}
                                          </div>
                                          <button
                                            onClick={() => handleVerify(f.id, doc.id, 'Verified')}
                                            className="p-2 bg-green-100 text-green-700 rounded-lg hover:bg-green-200 disabled:opacity-50"
                                            aria-label={`Verify ${doc.documentType}`}
                                            disabled={!isOnline || actionLoading === doc.id}
                                            title="Verify document"
                                          >
                                            <Check className="w-4 h-4" aria-hidden="true" />
                                          </button>
                                          <button
                                            onClick={() => handleVerify(f.id, doc.id, 'Rejected')}
                                            className="p-2 bg-red-100 text-red-700 rounded-lg hover:bg-red-200 disabled:opacity-50"
                                            aria-label={`Reject ${doc.documentType}`}
                                            disabled={!isOnline || actionLoading === doc.id}
                                            title="Reject document"
                                          >
                                            <X className="w-4 h-4" aria-hidden="true" />
                                          </button>
                                        </div>
                                      )}
                                    </div>
                                  ))}
                                </div>
                              )}
                            </>
                          )}

                          {/* ── Listings section ── */}
                          {section === 'listings' && (
                            <>
                              {!listings[f.id] ? (
                                <p className="text-sm text-gray-400">Loading listings…</p>
                              ) : listings[f.id].length === 0 ? (
                                <p className="text-sm text-gray-400">No crop listings for this farmer.</p>
                              ) : (
                                <div className="space-y-2">
                                  {listings[f.id].map((l) => (
                                    <div
                                      key={l.id}
                                      className="flex items-center justify-between bg-gray-50 rounded-lg px-4 py-3"
                                    >
                                      <div>
                                        <p className="font-medium text-sm text-gray-900">{l.cropType}</p>
                                        <p className="text-xs text-gray-500">
                                          {l.quantity} kg · ${l.pricePerUnit}/kg · {l.location}
                                        </p>
                                        <p className="text-xs text-gray-400">
                                          {new Date(l.createdAt).toLocaleDateString()}
                                        </p>
                                      </div>
                                      <div className="flex flex-col items-end gap-1">
                                        <span className={listingStatusClass[l.status] ?? 'badge-pending'}>
                                          {l.status.replace(/_/g, ' ')}
                                        </span>
                                        {l.rejectionReason && (
                                          <p className="text-xs text-red-600 max-w-40 text-right">
                                            {l.rejectionReason}
                                          </p>
                                        )}
                                      </div>
                                    </div>
                                  ))}
                                </div>
                              )}
                            </>
                          )}

                          {/* ── Account status actions ── */}
                          <div className="flex items-center gap-3 flex-wrap pt-4 mt-4 border-t border-gray-100">
                            {/* Activate — shown for Pending_Verification AND Inactive */}
                            {(f.status === 'Pending_Verification' || f.status === 'Inactive') && (
                              <div className="flex flex-col gap-1">
                                {!hasVerifiedDoc(f.id) && docsLoaded && (
                                  <p className="text-xs text-amber-700 flex items-center gap-1">
                                    <AlertTriangle className="w-3 h-3" aria-hidden="true" />
                                    Verify at least one document before activating.
                                  </p>
                                )}
                                <button
                                  onClick={() => handleStatusChange(f.id, 'Active')}
                                  className="btn-primary disabled:opacity-50"
                                  disabled={!isOnline || actionLoading === f.id || !hasVerifiedDoc(f.id)}
                                  aria-label={`Activate ${f.name}`}
                                >
                                  {actionLoading === f.id ? 'Activating…' : 'Activate Farmer'}
                                </button>
                              </div>
                            )}

                            {/* Deactivate — shown for Active farmers */}
                            {f.status === 'Active' && (
                              <button
                                onClick={() => handleStatusChange(f.id, 'Inactive')}
                                className="btn-danger disabled:opacity-50"
                                disabled={!isOnline || actionLoading === f.id}
                                aria-label={`Deactivate ${f.name}`}
                              >
                                {actionLoading === f.id ? 'Deactivating…' : 'Deactivate Farmer'}
                              </button>
                            )}
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>

            {/* ── Pagination ── */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between mt-6">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="btn-secondary flex items-center gap-1 disabled:opacity-40"
                  aria-label="Previous page"
                >
                  <ChevronLeft className="w-4 h-4" aria-hidden="true" />
                  Previous
                </button>
                <span className="text-sm text-gray-500">
                  Page {page + 1} of {totalPages}
                </span>
                <button
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  className="btn-secondary flex items-center gap-1 disabled:opacity-40"
                  aria-label="Next page"
                >
                  Next
                  <ChevronRight className="w-4 h-4" aria-hidden="true" />
                </button>
              </div>
            )}
          </>
        )}
      </main>
    </div>
  );
};

export default FarmerManagementPage;
