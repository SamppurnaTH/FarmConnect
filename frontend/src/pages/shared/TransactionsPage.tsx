import React, { useState, useEffect } from 'react';
import { NavMenu } from '../../components/NavMenu';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import CountdownTimer from '../../components/CountdownTimer';
import { transactionsApi } from '../../api/transactions';
import { cropsApi } from '../../api/crops';
import { farmersApi } from '../../api/farmers';
import { useAuthStore } from '../../stores/authStore';
import { useToast } from '../../components/ToastProvider';
import { useIsOnline } from '../../components/ConnectivityBanner';
import { usePolling } from '../../hooks/usePolling';
import type { Transaction, Payment, PaymentMethod } from '../../types';
import { Loader2 } from 'lucide-react';

/**
 * TransactionsPage — Requirement 7.1-7.6
 *
 * Role-aware loading:
 * - Farmer: loads their own orders → extracts orderIds → fetches matching transactions
 * - Trader: loads their own orders → extracts orderIds → fetches matching transactions
 * - Market Officer / Admin / Auditor: loads all transactions
 */
const TransactionsPage: React.FC = () => {
  const { role, userId } = useAuthStore();
  const { showToast } = useToast();
  const isOnline = useIsOnline();

  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [paymentState, setPaymentState] = useState<
    Record<string, {
      method: PaymentMethod;
      paymentId?: string;
      polling?: boolean;
      status?: string;
      failureReason?: string;
    }>
  >({});

  // ── Load transactions filtered by role ────────────────────────────────────
  useEffect(() => {
    if (!userId) return;

    const load = async () => {
      setLoading(true);
      try {
        if (role === 'Farmer') {
          // Resolve farmer profile → get their orders → filter transactions
          const profile = await farmersApi.getMyProfile();
          const orders = await cropsApi.getFarmerOrders(profile.id);
          if (orders.length === 0) {
            setTransactions([]);
            return;
          }
          const orderIds = orders.map((o) => o.id);
          const txs = await transactionsApi.getTransactionsByOrderIds(orderIds);
          setTransactions(txs);
        } else if (role === 'Trader') {
          // Resolve trader profile ID first — getTraderOrders expects profile ID, not identity userId
          const { tradersApi } = await import('../api/traders');
          const profile = await tradersApi.getMyProfile();
          const orders = await cropsApi.getTraderOrders(profile.id);
          if (orders.length === 0) {
            setTransactions([]);
            return;
          }
          const orderIds = orders.map((o) => o.id);
          const txs = await transactionsApi.getTransactionsByOrderIds(orderIds);
          setTransactions(txs);
        } else {
          // Market Officer, Admin, Auditor — see all
          const txs = await transactionsApi.getTransactions();
          setTransactions(txs);
        }
      } catch {
        showToast('Failed to load transactions', 'error');
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [userId, role]);

  // ── Payment submission ────────────────────────────────────────────────────
  const handlePayment = async (txId: string) => {
    const method = paymentState[txId]?.method ?? 'Bank_Transfer';
    try {
      const paymentId = await transactionsApi.submitPayment(txId, { method });
      setPaymentState((p) => ({
        ...p,
        [txId]: { ...p[txId], paymentId, polling: true, status: 'Processing' },
      }));
    } catch {
      showToast('Payment submission failed', 'error');
    }
  };

  // ── Poll payments in Processing state ─────────────────────────────────────
  usePolling(
    async () => {
      for (const [txId, state] of Object.entries(paymentState)) {
        if (!state.polling || !state.paymentId) continue;
        try {
          const payment: Payment = await transactionsApi.getPayment(state.paymentId);
          if (payment.status === 'Completed' || payment.status === 'Failed') {
            setPaymentState((p) => ({
              ...p,
              [txId]: {
                ...p[txId],
                polling: false,
                status: payment.status,
                failureReason: payment.failureReason,
              },
            }));
            if (payment.status === 'Completed') {
              setTransactions((prev) =>
                prev.map((t) => (t.id === txId ? { ...t, status: 'Settled' } : t))
              );
              showToast('Payment completed!', 'success');
            } else {
              showToast(`Payment failed: ${payment.failureReason ?? 'Unknown error'}`, 'error');
            }
          }
        } catch { /* silence */ }
      }
    },
    10_000,
    Object.values(paymentState).some((s) => s.polling),
  );

  const statusColor: Record<string, string> = {
    Pending_Payment: 'badge-pending',
    Settled:         'badge-settled',
    Cancelled:       'badge-closed',
  };

  if (loading) return <><NavMenu /><LoadingSpinner /></>;

  return (
    <div className="min-h-screen bg-gray-50">
      <NavMenu />
      <main className="max-w-5xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">Transactions</h1>

        {transactions.length === 0 ? (
          <div className="card text-center py-12 text-gray-500">No transactions found.</div>
        ) : (
          <div className="space-y-4">
            {transactions.map((tx) => {
              const state = paymentState[tx.id];
              const expired = new Date(tx.expiresAt) <= new Date();

              return (
                <div key={tx.id} className="card" data-testid="transaction-row">
                  <div className="flex items-start justify-between flex-wrap gap-4">
                    <div>
                      <p className="text-xs text-gray-400 font-mono mb-1">{tx.id}</p>
                      <div className="flex items-center gap-3">
                        <span className="text-xl font-bold text-gray-900">
                          ${tx.amount.toLocaleString()}
                        </span>
                        <span className={statusColor[tx.status] ?? 'badge-pending'}>
                          {tx.status.replace(/_/g, ' ')}
                        </span>
                      </div>
                      <p className="text-sm text-gray-500 mt-1">Order: {tx.orderId}</p>
                    </div>

                    {tx.status === 'Pending_Payment' && (
                      <div className="text-right">
                        <p className="text-xs text-gray-500 mb-1">Expires in</p>
                        <CountdownTimer
                          expiresAt={tx.expiresAt}
                          onExpire={() =>
                            setTransactions((prev) =>
                              prev.map((t) =>
                                t.id === tx.id ? { ...t, status: 'Cancelled' } : t
                              )
                            )
                          }
                        />
                      </div>
                    )}
                  </div>

                  {/* Payment form — only for Trader/Farmer on pending transactions */}
                  {tx.status === 'Pending_Payment' &&
                    !expired &&
                    (role === 'Trader' || role === 'Farmer') && (
                      <div className="mt-4 pt-4 border-t border-gray-100">
                        {state?.status === 'Processing' || state?.polling ? (
                          <div className="flex items-center gap-2 text-yellow-700 text-sm">
                            <Loader2 className="w-4 h-4 animate-spin" aria-hidden="true" />
                            Processing payment…
                          </div>
                        ) : state?.status === 'Completed' ? (
                          <p className="text-green-600 text-sm font-medium">✓ Payment completed</p>
                        ) : state?.status === 'Failed' ? (
                          <p className="text-red-600 text-sm">
                            ✗ Payment failed: {state.failureReason}
                          </p>
                        ) : (
                          <div className="flex items-center gap-3 flex-wrap">
                            <select
                              className="input-field w-auto"
                              aria-label="Payment method"
                              value={state?.method ?? 'Bank_Transfer'}
                              onChange={(e) =>
                                setPaymentState((p) => ({
                                  ...p,
                                  [tx.id]: {
                                    ...p[tx.id],
                                    method: e.target.value as PaymentMethod,
                                  },
                                }))
                              }
                            >
                              <option value="Bank_Transfer">Bank Transfer</option>
                              <option value="Mobile_Money">Mobile Money</option>
                              <option value="Card">Card</option>
                            </select>
                            <button
                              onClick={() => handlePayment(tx.id)}
                              className="btn-primary"
                              disabled={!isOnline}
                              aria-label={`Submit payment for transaction ${tx.id}`}
                            >
                              Pay Now
                            </button>
                          </div>
                        )}
                      </div>
                    )}
                </div>
              );
            })}
          </div>
        )}
      </main>
    </div>
  );
};

export default TransactionsPage;
