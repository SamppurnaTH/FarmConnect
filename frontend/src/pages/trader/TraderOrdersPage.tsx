import React, { useEffect, useState } from 'react';
import { NavMenu } from '../../components/NavMenu';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { cropsApi } from '../../api/crops';
import { tradersApi } from '../../api/traders';
import { useAuthStore } from '../../stores/authStore';
import { useToast } from '../../components/ToastProvider';
import { useIsOnline } from '../../components/ConnectivityBanner';
import type { Order } from '../../types';
import { X } from 'lucide-react';

const STATUS_BADGE: Record<string, string> = {
  Pending:   'badge-pending',
  Confirmed: 'badge-active',
  Declined:  'badge-rejected',
  Cancelled: 'badge-closed',
};

/**
 * TraderOrdersPage
 *
 * Resolves the trader profile ID from the JWT userId before fetching orders.
 * The GET /orders?traderId= endpoint expects the trader profile ID, not the identity userId.
 * Traders can cancel their own pending orders.
 */
const TraderOrdersPage: React.FC = () => {
  const { userId } = useAuthStore();
  const { showToast } = useToast();
  const isOnline = useIsOnline();

  const [traderId, setTraderId]   = useState<string | null>(null);
  const [orders, setOrders]       = useState<Order[]>([]);
  const [loading, setLoading]     = useState(true);
  const [cancelling, setCancelling] = useState<string | null>(null);
  const [error, setError]         = useState<string | null>(null);

  // ── Load trader profile then orders ──────────────────────────────────────
  useEffect(() => {
    if (!userId) return;

    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const profile = await tradersApi.getMyProfile();
        setTraderId(profile.id);
        // Pass trader profile ID — not identity userId
        const data = await cropsApi.getTraderOrders(profile.id);
        setOrders(data);
      } catch {
        setError('Failed to load orders. Please refresh the page.');
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [userId]);

  // ── Cancel order ──────────────────────────────────────────────────────────
  const handleCancel = async (orderId: string) => {
    setCancelling(orderId);
    try {
      await cropsApi.cancelOrder(orderId);
      setOrders((prev) =>
        prev.map((o) => o.id === orderId ? { ...o, status: 'Cancelled' as const } : o)
      );
      showToast('Order cancelled', 'info');
    } catch {
      showToast('Failed to cancel order. Please try again.', 'error');
    } finally {
      setCancelling(null);
    }
  };

  if (loading) return <><NavMenu /><LoadingSpinner /></>;

  return (
    <div className="min-h-screen bg-gray-50">
      <NavMenu />
      <main className="max-w-4xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">My Orders</h1>

        {error && (
          <div role="alert" className="card text-center py-8 text-red-600 mb-4">
            {error}
          </div>
        )}

        {!error && orders.length === 0 ? (
          <div className="card text-center py-12 text-gray-500">
            No orders placed yet. Browse listings to place your first order.
          </div>
        ) : (
          <div className="space-y-3">
            {orders.map((o) => (
              <div key={o.id} className="card flex items-center justify-between gap-4 flex-wrap">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-3 flex-wrap">
                    <span className={STATUS_BADGE[o.status] ?? 'badge-pending'}>
                      {o.status}
                    </span>
                    <span className="font-medium text-gray-900">{o.quantity} kg</span>
                    <span className="text-xs text-gray-400 font-mono">
                      Order {o.id.slice(0, 8)}…
                    </span>
                  </div>
                  <p className="text-xs text-gray-400 mt-1">
                    Listing {o.listingId.slice(0, 8)}… · Placed {new Date(o.createdAt).toLocaleDateString()}
                  </p>
                  {o.status === 'Confirmed' && (
                    <p className="text-xs text-green-700 mt-1">
                      ✓ Confirmed — check Transactions to complete payment.
                    </p>
                  )}
                  {o.status === 'Declined' && (
                    <p className="text-xs text-red-600 mt-1">
                      Order was declined by the farmer.
                    </p>
                  )}
                </div>

                {/* Cancel button — only for Pending orders */}
                {o.status === 'Pending' && (
                  <button
                    onClick={() => handleCancel(o.id)}
                    className="btn-danger text-xs flex items-center gap-1 disabled:opacity-50 shrink-0"
                    disabled={!isOnline || cancelling === o.id}
                    aria-label={`Cancel order ${o.id.slice(0, 8)}`}
                  >
                    <X className="w-3 h-3" aria-hidden="true" />
                    {cancelling === o.id ? 'Cancelling…' : 'Cancel'}
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  );
};

export default TraderOrdersPage;
