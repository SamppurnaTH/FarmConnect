import React, { useEffect, useState, useCallback } from 'react';
import { NavMenu } from '../../components/NavMenu';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { FieldError } from '../../components/FieldError';
import { cropsApi } from '../../api/crops';
import { tradersApi } from '../../api/traders';
import { useAuthStore } from '../../stores/authStore';
import { useToast } from '../../components/ToastProvider';
import { useIsOnline } from '../../components/ConnectivityBanner';
import type { CropListing } from '../../types';
import { SlidersHorizontal, ShoppingCart, Loader2 } from 'lucide-react';

/**
 * BrowseListingsPage — Requirement 6.1-6.5
 *
 * Resolves the trader's profile ID from the JWT userId before placing orders.
 * The OrderRequest.buyerId must be the trader profile ID, not the identity userId.
 */
const BrowseListingsPage: React.FC = () => {
  const { showToast } = useToast();
  const isOnline = useIsOnline();
  const { userId } = useAuthStore();

  const [traderId, setTraderId] = useState<string | null>(null);
  const [listings, setListings] = useState<CropListing[]>([]);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState({
    cropType: '', location: '', minPrice: '', maxPrice: '',
  });
  const [orderQty, setOrderQty]     = useState<Record<string, string>>({});
  const [ordering, setOrdering]     = useState<string | null>(null);
  const [orderError, setOrderError] = useState<Record<string, string>>({});

  // ── Resolve trader profile ID once on mount ───────────────────────────────
  useEffect(() => {
    if (!userId) return;
    tradersApi.getMyProfile()
      .then((profile) => setTraderId(profile.id))
      .catch(() => showToast('Failed to load trader profile', 'error'));
  }, [userId]);

  // ── Fetch listings ────────────────────────────────────────────────────────
  const fetchListings = useCallback(async () => {
    setLoading(true);
    try {
      const params = {
        ...(filters.cropType  && { cropType:  filters.cropType }),
        ...(filters.location  && { location:  filters.location }),
        ...(filters.minPrice  && { minPrice:  Number(filters.minPrice) }),
        ...(filters.maxPrice  && { maxPrice:  Number(filters.maxPrice) }),
      };
      const data = await cropsApi.getListings(params);
      // Only show Active listings
      setListings(data.filter((l) => l.status === 'Active'));
    } finally {
      setLoading(false);
    }
  }, [filters]);

  useEffect(() => { fetchListings(); }, [fetchListings]);

  // ── Place order ───────────────────────────────────────────────────────────
  const handleOrder = async (listingId: string) => {
    if (!traderId) {
      showToast('Trader profile not loaded. Please refresh.', 'error');
      return;
    }

    const qty = Number(orderQty[listingId]);
    if (!qty || qty <= 0) {
      setOrderError((p) => ({ ...p, [listingId]: 'Please enter a valid quantity.' }));
      return;
    }

    const listing = listings.find((l) => l.id === listingId);
    const available = listing?.availableQuantity ?? listing?.quantity ?? 0;
    if (qty > available) {
      setOrderError((p) => ({
        ...p,
        [listingId]: `Quantity exceeds available stock (${available} kg).`,
      }));
      return;
    }

    setOrdering(listingId);
    setOrderError((p) => ({ ...p, [listingId]: '' }));
    try {
      // Use traderId (profile ID), not userId (identity ID)
      await cropsApi.placeOrder(listingId, traderId, qty);
      showToast('Order placed — waiting for farmer to confirm.', 'success');
      setOrderQty((p) => ({ ...p, [listingId]: '' }));
      // Refresh listings to show updated available quantity
      fetchListings();
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      if (status === 422) {
        setOrderError((p) => ({
          ...p,
          [listingId]: `Insufficient quantity. Available: ${available} kg.`,
        }));
      } else {
        setOrderError((p) => ({ ...p, [listingId]: 'Order failed. Please try again.' }));
      }
    } finally {
      setOrdering(null);
    }
  };

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <div className="min-h-screen bg-gray-50">
      <NavMenu />
      <main className="max-w-6xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">Browse Crop Listings</h1>

        {/* Filters */}
        <div className="card mb-6">
          <div className="flex items-center gap-2 mb-4">
            <SlidersHorizontal className="w-4 h-4 text-gray-500" aria-hidden="true" />
            <h2 className="font-medium text-gray-700">Filters</h2>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {([
              ['cropType', 'Crop Type',    'text'],
              ['location', 'Location',     'text'],
              ['minPrice', 'Min Price ($)', 'number'],
              ['maxPrice', 'Max Price ($)', 'number'],
            ] as [keyof typeof filters, string, string][]).map(([key, label, type]) => (
              <div key={key}>
                <label htmlFor={`filter-${key}`} className="block text-xs font-medium text-gray-500 mb-1">
                  {label}
                </label>
                <input
                  id={`filter-${key}`}
                  type={type}
                  className="input-field text-sm"
                  value={filters[key]}
                  onChange={(e) => setFilters((p) => ({ ...p, [key]: e.target.value }))}
                />
              </div>
            ))}
          </div>
        </div>

        {loading ? (
          <LoadingSpinner />
        ) : listings.length === 0 ? (
          <div className="card text-center py-12 text-gray-500">
            No active listings match your filters.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {listings.map((l) => (
              <div key={l.id} className="card">
                <div className="flex items-start justify-between mb-2">
                  <h3 className="font-semibold text-gray-900">{l.cropType}</h3>
                  <span className="badge-active">Active</span>
                </div>

                <dl className="text-sm text-gray-600 space-y-1 mb-4">
                  <div className="flex justify-between">
                    <dt>Available</dt>
                    <dd className="font-medium">{l.availableQuantity ?? l.quantity} kg</dd>
                  </div>
                  <div className="flex justify-between">
                    <dt>Price</dt>
                    <dd className="font-medium">${l.pricePerUnit}/kg</dd>
                  </div>
                  <div className="flex justify-between">
                    <dt>Location</dt>
                    <dd className="font-medium">{l.location}</dd>
                  </div>
                  <div className="flex justify-between">
                    <dt>Total value</dt>
                    <dd className="font-medium text-primary-700">
                      ${((l.availableQuantity ?? l.quantity) * l.pricePerUnit).toLocaleString()}
                    </dd>
                  </div>
                </dl>

                <div className="flex gap-2">
                  <input
                    type="number"
                    className="input-field flex-1"
                    placeholder="Quantity (kg)"
                    min="1"
                    max={l.availableQuantity ?? l.quantity}
                    aria-label={`Order quantity for ${l.cropType}`}
                    value={orderQty[l.id] ?? ''}
                    onChange={(e) => {
                      setOrderQty((p) => ({ ...p, [l.id]: e.target.value }));
                      setOrderError((p) => ({ ...p, [l.id]: '' }));
                    }}
                  />
                  <button
                    onClick={() => handleOrder(l.id)}
                    className="btn-primary flex items-center gap-1 whitespace-nowrap disabled:opacity-50"
                    disabled={ordering === l.id || !isOnline || !traderId}
                    aria-label={`Place order for ${l.cropType}`}
                  >
                    {ordering === l.id
                      ? <Loader2 className="w-4 h-4 animate-spin" aria-hidden="true" />
                      : <ShoppingCart className="w-4 h-4" aria-hidden="true" />}
                    Order
                  </button>
                </div>
                {orderError[l.id] && <FieldError message={orderError[l.id]} />}
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  );
};

export default BrowseListingsPage;
