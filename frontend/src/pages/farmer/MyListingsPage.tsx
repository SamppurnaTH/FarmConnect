import React, { useEffect, useState } from 'react';
import { NavMenu } from '../../components/NavMenu';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { cropsApi } from '../../api/crops';
import { farmersApi } from '../../api/farmers';
import { useAuthStore } from '../../stores/authStore';
import { useToast } from '../../components/ToastProvider';
import { useIsOnline } from '../../components/ConnectivityBanner';
import type { CropListing, Order } from '../../types';
import { Plus, Check, X, ChevronDown, ChevronUp } from 'lucide-react';

/**
 * MyListingsPage — Requirement 5.1-5.8
 */
const MyListingsPage: React.FC = () => {
  const { userId } = useAuthStore();
  const { showToast } = useToast();
  const isOnline = useIsOnline();

  const [farmerId, setFarmerId] = useState<string | null>(null);
  const [listings, setListings] = useState<CropListing[]>([]);
  const [orders, setOrders] = useState<Record<string, Order[]>>({});
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState({ cropType: '', quantity: '', pricePerUnit: '', location: '' });
  const [formError, setFormError] = useState<string | null>(null);

  // Resolve farmer profile ID from JWT userId, then load listings
  const load = async (resolvedFarmerId?: string) => {
    const fid = resolvedFarmerId ?? farmerId;
    if (!fid) return;
    setLoading(true);
    try {
      const data = await cropsApi.getMyListings(fid);
      setListings(data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!userId) return;
    farmersApi.getMyProfile()
      .then((profile) => {
        setFarmerId(profile.id);
        return cropsApi.getMyListings(profile.id);
      })
      .then(setListings)
      .catch(() => showToast('Failed to load listings', 'error'))
      .finally(() => setLoading(false));
  }, [userId]);

  const loadOrders = async (listingId: string) => {
    try {
      const o = await cropsApi.getOrdersForListing(listingId);
      setOrders((prev) => ({ ...prev, [listingId]: o }));
    } catch { /* silence */ }
  };

  const toggle = (id: string) => {
    const next = expandedId === id ? null : id;
    setExpandedId(next);
    if (next) loadOrders(next);
  };

  const handleCreate = async () => {
    setFormError(null);
    const qty = Number(form.quantity);
    const price = Number(form.pricePerUnit);
    if (!form.cropType || qty <= 0 || price <= 0 || !form.location) {
      setFormError('All fields are required. Quantity and price must be greater than 0.');
      return;
    }
    try {
      await cropsApi.createListing({ cropType: form.cropType, quantity: qty, pricePerUnit: price, location: form.location });
      showToast('Listing submitted for approval', 'success');
      setShowCreate(false);
      setForm({ cropType: '', quantity: '', pricePerUnit: '', location: '' });
      load(farmerId ?? undefined);
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      if (status === 422) {
        setFormError('All mandatory documents must be verified before creating a listing.');
      } else {
        setFormError('Failed to create listing. Please try again.');
      }
    }
  };

  const handleAccept = async (orderId: string, listingId: string) => {
    try {
      await cropsApi.acceptOrder(orderId);
      showToast('Order accepted', 'success');
      loadOrders(listingId);
      load(farmerId ?? undefined);
    } catch { showToast('Failed to accept order', 'error'); }
  };

  const handleDecline = async (orderId: string, listingId: string) => {
    try {
      await cropsApi.declineOrder(orderId);
      showToast('Order declined', 'info');
      loadOrders(listingId);
    } catch { showToast('Failed to decline order', 'error'); }
  };

  const statusClass: Record<string, string> = {
    Pending_Approval: 'badge-pending',
    Active:           'badge-active',
    Rejected:         'badge-rejected',
    Closed:           'badge-closed',
  };

  if (loading) return <><NavMenu /><LoadingSpinner /></>;

  return (
    <div className="min-h-screen bg-gray-50">
      <NavMenu />
      <main className="max-w-5xl mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold text-gray-900">My Crop Listings</h1>
          <button className="btn-primary flex items-center gap-2" onClick={() => setShowCreate((s) => !s)}>
            <Plus className="w-4 h-4" /> New Listing
          </button>
        </div>

        {/* Create form */}
        {showCreate && (
          <div className="card mb-6">
            <h2 className="font-semibold text-gray-900 mb-4">Create New Listing</h2>
            {formError && <div role="alert" className="mb-3 p-3 bg-red-50 border border-red-200 rounded text-red-700 text-sm">{formError}</div>}
            <div className="grid grid-cols-2 gap-4">
              {[
                ['cropType', 'Crop Type', 'text'],
                ['quantity', 'Quantity (kg)', 'number'],
                ['pricePerUnit', 'Price per Unit ($)', 'number'],
                ['location', 'Location', 'text'],
              ].map(([f, label, type]) => (
                <div key={f}>
                  <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
                  <input
                    type={type}
                    className="input-field"
                    value={form[f as keyof typeof form]}
                    min={type === 'number' ? '0.01' : undefined}
                    onChange={(e) => setForm((p) => ({ ...p, [f]: e.target.value }))}
                  />
                </div>
              ))}
            </div>
            <div className="flex gap-3 mt-4">
              <button onClick={handleCreate} className="btn-primary" disabled={!isOnline}>Submit for Approval</button>
              <button onClick={() => setShowCreate(false)} className="btn-secondary">Cancel</button>
            </div>
          </div>
        )}

        {/* Listings */}
        {listings.length === 0 ? (
          <div className="card text-center py-12 text-gray-500">No listings yet. Create your first listing above.</div>
        ) : (
          <div className="space-y-4">
            {listings.map((l) => (
              <div key={l.id} className="bg-white rounded-xl border border-gray-200">
                <button
                  onClick={() => toggle(l.id)}
                  className="w-full flex items-center justify-between px-6 py-4 text-left"
                  aria-expanded={expandedId === l.id}
                >
                  <div>
                    <span className="font-semibold text-gray-900">{l.cropType}</span>
                    <span className="ml-3 text-sm text-gray-500">{l.availableQuantity ?? l.quantity} kg @ ${l.pricePerUnit}/kg · {l.location}</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className={statusClass[l.status] ?? 'badge-pending'}>{l.status.replace(/_/g,' ')}</span>
                    {expandedId === l.id ? <ChevronUp className="w-4 h-4 text-gray-400" /> : <ChevronDown className="w-4 h-4 text-gray-400" />}
                  </div>
                </button>

                {expandedId === l.id && (
                  <div className="px-6 pb-4 border-t border-gray-100">
                    {l.rejectionReason && (
                      <p className="text-sm text-red-600 mt-3">Rejection reason: {l.rejectionReason}</p>
                    )}
                    <h3 className="font-medium text-gray-800 mt-3 mb-2">Pending Orders</h3>
                    {(orders[l.id] ?? []).filter((o) => o.status === 'Pending').length === 0 ? (
                      <p className="text-sm text-gray-400">No pending orders</p>
                    ) : (
                      <ul className="space-y-2">
                        {(orders[l.id] ?? []).filter((o) => o.status === 'Pending').map((o) => (
                          <li key={o.id} className="flex items-center justify-between bg-gray-50 rounded-lg px-4 py-2">
                            <span className="text-sm text-gray-700">
                              {o.quantity} kg — ordered {new Date(o.createdAt).toLocaleDateString()}
                            </span>
                            <div className="flex gap-2">
                              <button
                                onClick={() => handleAccept(o.id, l.id)}
                                className="flex items-center gap-1 text-xs btn-primary py-1.5 px-3"
                                aria-label={`Accept order for ${o.quantity} kg`}
                              >
                                <Check className="w-3 h-3" /> Accept
                              </button>
                              <button
                                onClick={() => handleDecline(o.id, l.id)}
                                className="flex items-center gap-1 text-xs btn-danger py-1.5 px-3"
                                aria-label={`Decline order for ${o.quantity} kg`}
                              >
                                <X className="w-3 h-3" /> Decline
                              </button>
                            </div>
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  );
};

export default MyListingsPage;
