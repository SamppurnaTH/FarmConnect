import React, { useEffect, useState } from 'react';
import { NavMenu } from '../../components/NavMenu';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { cropsApi } from '../../api/crops';
import { useToast } from '../../components/ToastProvider';
import { useIsOnline } from '../../components/ConnectivityBanner';
import type { CropListing } from '../../types';
import { Check, X } from 'lucide-react';

/**
 * ListingApprovalPage — Requirement 13.1-13.4
 */
const ListingApprovalPage: React.FC = () => {
  const { showToast } = useToast();
  const isOnline = useIsOnline();
  const [listings, setListings] = useState<CropListing[]>([]);
  const [loading, setLoading] = useState(true);
  const [reasons, setReasons] = useState<Record<string, string>>({});
  const [reasonErrors, setReasonErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    cropsApi.getListings().then((all) =>
      setListings(all.filter((l) => l.status === 'Pending_Approval'))
    ).finally(() => setLoading(false));
  }, []);

  const handleAction = async (id: string, action: 'Active' | 'Rejected') => {
    if (action === 'Rejected' && !reasons[id]?.trim()) {
      setReasonErrors((p) => ({ ...p, [id]: 'Rejection reason is required.' }));
      return;
    }
    try {
      await cropsApi.updateListing(id, { status: action, rejectionReason: reasons[id] });
      setListings((prev) => prev.filter((l) => l.id !== id));
      showToast(`Listing ${action === 'Active' ? 'approved' : 'rejected'}`, 'success');
    } catch { showToast('Action failed', 'error'); }
  };

  if (loading) return <><NavMenu /><LoadingSpinner /></>;

  return (
    <div className="min-h-screen bg-gray-50">
      <NavMenu />
      <main className="max-w-4xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">Listing Approval Queue</h1>
        <p className="text-gray-500 mb-4">{listings.length} listing(s) pending approval</p>

        {listings.length === 0 ? (
          <div className="card text-center py-12 text-gray-500">No listings pending approval. ✓</div>
        ) : (
          <div className="space-y-4">
            {listings.map((l) => (
              <div key={l.id} className="card">
                <div className="flex items-start justify-between mb-3">
                  <div>
                    <h3 className="font-semibold text-gray-900">{l.cropType}</h3>
                    <p className="text-sm text-gray-500">{l.quantity} kg · ${l.pricePerUnit}/kg · {l.location}</p>
                    <p className="text-xs text-gray-400 mt-1">Farmer: {l.farmerId}</p>
                  </div>
                  <span className="badge-pending">Pending Approval</span>
                </div>

                <div className="flex items-center gap-3 flex-wrap">
                  <input
                    type="text"
                    className="input-field flex-1"
                    placeholder="Rejection reason (required for rejection)"
                    aria-label={`Rejection reason for ${l.cropType} listing`}
                    value={reasons[l.id] ?? ''}
                    onChange={(e) => setReasons((p) => ({ ...p, [l.id]: e.target.value }))}
                  />
                  {reasonErrors[l.id] && <p className="text-xs text-red-500 w-full">{reasonErrors[l.id]}</p>}
                  <button
                    onClick={() => handleAction(l.id, 'Active')}
                    className="btn-primary flex items-center gap-2"
                    disabled={!isOnline}
                    aria-label={`Approve ${l.cropType} listing`}
                  >
                    <Check className="w-4 h-4" /> Approve
                  </button>
                  <button
                    onClick={() => handleAction(l.id, 'Rejected')}
                    className="btn-danger flex items-center gap-2"
                    disabled={!isOnline}
                    aria-label={`Reject ${l.cropType} listing`}
                  >
                    <X className="w-4 h-4" /> Reject
                  </button>
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
