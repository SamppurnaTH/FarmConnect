import React, { useEffect, useState, useCallback } from 'react';
import { NavMenu } from '../../components/NavMenu';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { FieldError } from '../../components/FieldError';
import { cropsApi } from '../../api/crops';
import { useAuthStore } from '../../stores/authStore';
import { useToast } from '../../components/ToastProvider';
import { useIsOnline } from '../../components/ConnectivityBanner';
import type { CropListing } from '../../types';
import { SlidersHorizontal, ShoppingCart, Loader2 } from 'lucide-react';

/**
 * BrowseListingsPage — Requirement 6.1-6.5
 */
const BrowseListingsPage: React.FC = () => {
  const { showToast } = useToast();
  const isOnline = useIsOnline();
  const { userId } = useAuthStore();
  const [listings, setListings] = useState<CropListing[]>([]);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState({ cropType: '', location: '', minPrice: '', maxPrice: '' });
  const [orderQty, setOrderQty] = useState<Record<string, string>>({});
  const [ordering, setOrdering] = useState<string | null>(null);
  const [orderError, setOrderError] = useState<Record<string, string>>({});

  const fetchListings = useCallback(async () => {
    setLoading(true);
    try {
      const params = {
        ...(filters.cropType && { cropType: filters.cropType }),
        ...(filters.location && { location: filters.location }),
        ...(filters.minPrice && { minPrice: Number(filters.minPrice) }),
        ...(filters.maxPrice && { maxPrice: Number(filters.maxPrice) }),
      };
      const data = await cropsApi.getListings(params);
      setListings(data.filter((l) => l.status === 'Active'));
    } finally {
      setLoading(false);
    }
  }, [filters]);

  useEffect(() => { fetchListings(); }, [fetchListings]);

  const handleOrder = async (listingId: string) => {
    const qty = Number(orderQty[listingId]);
    if (!qty || qty <= 0) {
      setOrderError((p) => ({ ...p, [listingId]: 'Please enter a valid quantity.' }));
      return;
    }
    setOrdering(listingId);
    setOrderError((p) => ({ ...p, [listingId]: '' }));
    try {
      await cropsApi.placeOrder(listingId, userId || '', qty);
      showToast('Order placed successfully!', 'success');
      setOrderQty((p) => ({ ...p, [listingId]: '' }));
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      if (status === 422) {
        const listing = listings.find((l) => l.id === listingId);
        setOrderError((p) => ({
          ...p,
          [listingId]: `Insufficient quantity. Available: ${listing?.availableQuantity ?? '?'} kg.`,
        }));
      } else {
        setOrderError((p) => ({ ...p, [listingId]: 'Order failed. Please try again.' }));
      }
    } finally {
      setOrdering(null);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <NavMenu />
      <main className="max-w-6xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">Browse Crop Listings</h1>

        {/* Filters */}
        <div className="card mb-6">
          <div className="flex items-center gap-2 mb-4">
            <SlidersHorizontal className="w-4 h-4 text-gray-500" />
            <h2 className="font-medium text-gray-700">Filters</h2>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {[
              ['cropType', 'Crop Type', 'text'],
              ['location', 'Location', 'text'],
              ['minPrice', 'Min Price ($)', 'number'],
              ['maxPrice', 'Max Price ($)', 'number'],
            ].map(([key, label, type]) => (
              <div key={key}>
                <label className="block text-xs font-medium text-gray-500 mb-1">{label}</label>
                <input
                  type={type}
                  className="input-field text-sm"
                  aria-label={label}
                  value={filters[key as keyof typeof filters]}
                  onChange={(e) => setFilters((p) => ({ ...p, [key]: e.target.value }))}
                />
              </div>
            ))}
          </div>
        </div>

        {loading ? (
          <LoadingSpinner />
        ) : listings.length === 0 ? (
          <div className="card text-center py-12 text-gray-500">No active listings match your filters.</div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {listings.map((l) => (
              <div key={l.id} className="card">
                <div className="flex items-start justify-between mb-2">
                  <h3 className="font-semibold text-gray-900">{l.cropType}</h3>
                  <span className="badge-active">Active</span>
                </div>
                <dl className="text-sm text-gray-600 space-y-1 mb-4">
                  <div className="flex justify-between"><dt>Available</dt><dd className="font-medium">{l.availableQuantity ?? l.quantity} kg</dd></div>
                  <div className="flex justify-between"><dt>Price</dt><dd className="font-medium">${l.pricePerUnit}/kg</dd></div>
                  <div className="flex justify-between"><dt>Location</dt><dd className="font-medium">{l.location}</dd></div>
                </dl>

                <div className="flex gap-2">
                  <input
                    type="number"
                    className="input-field flex-1"
                    placeholder="Quantity (kg)"
                    min="1"
                    aria-label={`Order quantity for ${l.cropType}`}
                    value={orderQty[l.id] ?? ''}
                    onChange={(e) => setOrderQty((p) => ({ ...p, [l.id]: e.target.value }))}
                  />
                  <button
                    onClick={() => handleOrder(l.id)}
                    className="btn-primary flex items-center gap-1 whitespace-nowrap"
                    disabled={ordering === l.id || !isOnline}
                    aria-label={`Place order for ${l.cropType}`}
                  >
                    {ordering === l.id ? <Loader2 className="w-4 h-4 animate-spin" /> : <ShoppingCart className="w-4 h-4" />}
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
