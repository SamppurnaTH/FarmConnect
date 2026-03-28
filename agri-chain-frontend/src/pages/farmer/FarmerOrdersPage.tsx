import React, { useEffect, useState } from 'react';
import { NavMenu } from '../../components/NavMenu';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { cropsApi } from '../../api/crops';
import { useAuthStore } from '../../stores/authStore';
import type { Order } from '../../types';

const FarmerOrdersPage: React.FC = () => {
  const { userId } = useAuthStore();
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!userId) return;
    cropsApi.getTraderOrders(userId).then(setOrders).finally(() => setLoading(false));
  }, [userId]);

  const sc: Record<string, string> = { Pending: 'badge-pending', Confirmed: 'badge-active', Declined: 'badge-rejected', Cancelled: 'badge-closed' };

  if (loading) return <><NavMenu /><LoadingSpinner /></>;

  return (
    <div className="min-h-screen bg-gray-50">
      <NavMenu />
      <main className="max-w-4xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">My Orders</h1>
        {orders.length === 0 ? (
          <div className="card text-center py-12 text-gray-500">No orders found.</div>
        ) : (
          <div className="card overflow-hidden p-0">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  {['Order ID','Listing ID','Quantity','Status','Date'].map((h) => (
                    <th key={h} className="px-4 py-3 text-left font-semibold text-gray-700">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {orders.map((o) => (
                  <tr key={o.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 font-mono text-xs text-gray-500">{o.id.slice(0,8)}…</td>
                    <td className="px-4 py-3 font-mono text-xs text-gray-500">{o.listingId.slice(0,8)}…</td>
                    <td className="px-4 py-3 font-medium">{o.quantity} kg</td>
                    <td className="px-4 py-3"><span className={sc[o.status]}>{o.status}</span></td>
                    <td className="px-4 py-3 text-gray-500">{new Date(o.createdAt).toLocaleDateString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>
    </div>
  );
};

export default FarmerOrdersPage;
