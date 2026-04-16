import React, { useEffect, useState } from 'react';
import { NavMenu } from '../../components/NavMenu';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { cropsApi } from '../../api/crops';
import { useToast } from '../../components/ToastProvider';
import { useIsOnline } from '../../components/ConnectivityBanner';
import type { CropListing } from '../../types';
import { Check, X, RefreshCw } from 'lucide-react';

/**
 * ListingApprovalPage — Requirement 13.1-13.4
 * Market Officer approves or rejects pending crop listings.
 *
 * Uses GET /listings/pending (dedicated endpoint — no client-side filtering).
 * Uses PUT /listings/{id}/status (correct endpoint).
 * Farmer is notified by the backend on approval/rejection.
 */
const ListingApprovalPage: React.FC = () => {
  const { showToast } = useToast();
  const isOnline = useIsOnline();

  const [listings, setListings] = useState<CropListing[]>([]);
  const [loading, setLoading] = useState(true);
  const [reasons, setReasons] = useState<Record<string, string>>({});
  const [reasonErrors, setReasonErrors] = useState<Record<string, string>>({});
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  const load = async () => {
    setLoading(true);
    try {
      const data = await cropsApi.getPendingListings();
      setListings(data);
    } catch {
      showToast('Failed to load pending listings', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handleAction = async (id: string, action: 'Active' | 'Rejected') => {
    if (action === 'Rejected' && !reasons[id]?.trim()) {
      setReasonErrors((p) => ({ ...p, [id]: 'Rejection reason is required.' }));
      return;
    }
    setReasonErrors((p) => ({ ...p, [id]: '' }));
    setActionLoading(id);
    try {
      await cropsApi.updateListing(id, {
        status: action,
        rejectionReason: reasons[id],
      });
      setListings((prev) => prev.filter((l) => l.id !== id));
      showToast(
        action === 'Active' ? 'Listing approved — farmer has been notified.' : 'Listing rejected — farmer has been notified.',
        'success'
      );
    } catch {
      showToast('Action failed. Please try again.', 'error');
    } finally {
      setActionLoading(null);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <NavMenu />
      <main className="max-w-4xl mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Listing Approval Queue</h1>
            {!loading && (
              <p className="text-sm text-gray-500 mt-1">
                {listings.length} listing{listings.length !== 1 ? 's' : ''} pending approval
              </p>
            )}
          </div>
          <button
            onClick={load}
            className="btn-secondary flex items-center gap-2"
            aria-label="Refresh listing queue"
            disabled={loading}
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} aria-hidden="true" />
            Refresh
          </button>
        </div>

        {loading ? (
          <LoadingSpinner />
        ) : listings.length === 0 ? (
          <div className="card text-center py-12 text-gray-500">
            ✓ No listings pending approval.
          </div>
        ) : (
          <div className="space-y-4">
            {listings.map((l) => (
              <div key={l.id} className="card">
                {/* Listing details */}
                <div className="flex items-start justify-between mb-4">
                  <div>
                    <h3 className="font-semibold text-gray-900 text-lg">{l.cropType}</h3>
                    <div className="flex flex-wrap gap-x-4 gap-y-1 text-sm text-gray-600 mt-1">
                      <span>{l.quantity} kg available</span>
                      <span>${l.pricePerUnit}/kg</span>
                      <span>📍 {l.location}</span>
                    </div>
                    <p className="text-xs text-gray-400 font-mono mt-1">
                      Farmer ID: {l.farmerId}
                    </p>
                    <p className="text-xs text-gray-400 mt-0.5">
                      Submitted {new Date(l.createdAt).toLocaleDateString()}
                    </p>
                  </div>
                  <span className="badge-pending shrink-0">Pending Approval</span>
                </div>

                {/* Rejection reason input + action buttons */}
                <div className="border-t border-gray-100 pt-4">
                  <div className="flex items-start gap-3 flex-wrap">
                    <div className="flex-1 min-w-48">
                      <input
                        type="text"
                        className={`input-field ${reasonErrors[l.id] ? 'border-red-400' : ''}`}
                        placeholder="Rejection reason (required if rejecting)"
                        aria-label={`Rejection reason for ${l.cropType} listing`}
                        value={reasons[l.id] ?? ''}
                        onChange={(e) => {
                          setReasons((p) => ({ ...p, [l.id]: e.target.value }));
                          setReasonErrors((p) => ({ ...p, [l.id]: '' }));
                        }}
                      />
                      {reasonErrors[l.id] && (
                        <p className="text-xs text-red-500 mt-1">{reasonErrors[l.id]}</p>
                      )}
                    </div>

                    <div className="flex gap-2 shrink-0">
                      <button
                        onClick={() => handleAction(l.id, 'Active')}
                        className="btn-primary flex items-center gap-2 disabled:opacity-50"
                        disabled={!isOnline || actionLoading === l.id}
                        aria-label={`Approve ${l.cropType} listing`}
                      >
                        <Check className="w-4 h-4" aria-hidden="true" />
                        {actionLoading === l.id ? 'Processing…' : 'Approve'}
                      </button>
                      <button
                        onClick={() => handleAction(l.id, 'Rejected')}
                        className="btn-danger flex items-center gap-2 disabled:opacity-50"
                        disabled={!isOnline || actionLoading === l.id}
                        aria-label={`Reject ${l.cropType} listing`}
                      >
                        <X className="w-4 h-4" aria-hidden="true" />
                        Reject
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  );
};

export default ListingApprovalPage;
