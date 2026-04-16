import React, { useEffect, useState } from 'react';
import { NavMenu } from '../../components/NavMenu';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { subsidiesApi } from '../../api/subsidies';
import { useToast } from '../../components/ToastProvider';
import { useIsOnline } from '../../components/ConnectivityBanner';
import type { Disbursement } from '../../types';
import { CheckCircle, XCircle } from 'lucide-react';

const STATUS_BADGE: Record<string, string> = {
  Pending:  'badge-pending',
  Approved: 'badge-active',
  Rejected: 'badge-rejected',
  Disbursed:'badge-settled',
  Failed:   'badge-failed',
};

/**
 * DisbursementsPage — Requirement 8.7, 12.2, 12.3
 * Program Manager approves or rejects pending disbursements.
 * Rejection requires a mandatory reason.
 * Budget is reconciled automatically on rejection.
 */
const DisbursementsPage: React.FC = () => {
  const { showToast } = useToast();
  const isOnline = useIsOnline();

  const [disbursements, setDisbursements] = useState<Disbursement[]>([]);
  const [loading, setLoading]             = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [rejectReason, setRejectReason]   = useState<Record<string, string>>({});
  const [rejectError, setRejectError]     = useState<Record<string, string>>({});

  useEffect(() => {
    subsidiesApi.getDisbursements()
      .then(setDisbursements)
      .finally(() => setLoading(false));
  }, []);

  // ── Approve ───────────────────────────────────────────────────────────────
  const handleApprove = async (id: string) => {
    setActionLoading(id);
    try {
      await subsidiesApi.approveDisbursement(id);
      setDisbursements((prev) =>
        prev.map((d) => d.id === id ? { ...d, status: 'Approved' as const } : d)
      );
      showToast('Disbursement approved — farmer has been notified.', 'success');
    } catch {
      showToast('Approval failed. Please try again.', 'error');
    } finally {
      setActionLoading(null);
    }
  };

  // ── Reject ────────────────────────────────────────────────────────────────
  const handleReject = async (id: string) => {
    const reason = rejectReason[id]?.trim();
    if (!reason) {
      setRejectError((p) => ({ ...p, [id]: 'Rejection reason is required.' }));
      return;
    }
    setRejectError((p) => ({ ...p, [id]: '' }));
    setActionLoading(id);
    try {
      await subsidiesApi.rejectDisbursement(id, reason);
      setDisbursements((prev) =>
        prev.map((d) =>
          d.id === id
            ? { ...d, status: 'Rejected' as const, rejectionReason: reason }
            : d
        )
      );
      showToast('Disbursement rejected — budget reconciled, farmer notified.', 'success');
    } catch {
      showToast('Rejection failed. Please try again.', 'error');
    } finally {
      setActionLoading(null);
    }
  };

  if (loading) return <><NavMenu /><LoadingSpinner /></>;

  return (
    <div className="min-h-screen bg-gray-50">
      <NavMenu />
      <main className="max-w-5xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">Disbursements</h1>

        {disbursements.length === 0 ? (
          <div className="card text-center py-12 text-gray-500">No disbursements found.</div>
        ) : (
          <div className="space-y-3">
            {disbursements.map((d) => (
              <div key={d.id} className="card">
                {/* ── Summary row ── */}
                <div className="flex items-start justify-between gap-4 flex-wrap">
                  <div>
                    <div className="flex items-center gap-3 flex-wrap">
                      <span className={STATUS_BADGE[d.status] ?? 'badge-pending'}>
                        {d.status}
                      </span>
                      <span className="font-bold text-gray-900 text-lg">
                        ${d.amount.toLocaleString()}
                      </span>
                      <span className="text-sm text-gray-500">
                        Cycle: {d.programCycle}
                      </span>
                    </div>
                    <p className="text-xs text-gray-400 font-mono mt-1">
                      Farmer {d.farmerId.slice(0, 8)}… · Program {d.programId.slice(0, 8)}…
                    </p>
                    {d.rejectionReason && (
                      <p className="text-xs text-red-600 mt-1">
                        Rejection reason: {d.rejectionReason}
                      </p>
                    )}
                    {d.approvedAt && d.status === 'Approved' && (
                      <p className="text-xs text-gray-400 mt-1">
                        Approved {new Date(d.approvedAt).toLocaleDateString()}
                      </p>
                    )}
                  </div>
                </div>

                {/* ── Actions for Pending disbursements ── */}
                {d.status === 'Pending' && (
                  <div className="mt-4 pt-4 border-t border-gray-100">
                    <div className="flex items-start gap-3 flex-wrap">
                      {/* Rejection reason input */}
                      <div className="flex-1 min-w-48">
                        <input
                          type="text"
                          className={`input-field text-sm ${rejectError[d.id] ? 'border-red-400' : ''}`}
                          placeholder="Rejection reason (required if rejecting)"
                          aria-label={`Rejection reason for disbursement ${d.id.slice(0, 8)}`}
                          value={rejectReason[d.id] ?? ''}
                          onChange={(e) => {
                            setRejectReason((p) => ({ ...p, [d.id]: e.target.value }));
                            setRejectError((p) => ({ ...p, [d.id]: '' }));
                          }}
                        />
                        {rejectError[d.id] && (
                          <p className="text-xs text-red-500 mt-1">{rejectError[d.id]}</p>
                        )}
                      </div>

                      {/* Approve button */}
                      <button
                        onClick={() => handleApprove(d.id)}
                        className="btn-primary flex items-center gap-1.5 disabled:opacity-50 shrink-0"
                        disabled={!isOnline || actionLoading === d.id}
                        aria-label={`Approve disbursement for farmer ${d.farmerId.slice(0, 8)}`}
                      >
                        <CheckCircle className="w-4 h-4" aria-hidden="true" />
                        {actionLoading === d.id ? 'Processing…' : 'Approve'}
                      </button>

                      {/* Reject button */}
                      <button
                        onClick={() => handleReject(d.id)}
                        className="btn-danger flex items-center gap-1.5 disabled:opacity-50 shrink-0"
                        disabled={!isOnline || actionLoading === d.id}
                        aria-label={`Reject disbursement for farmer ${d.farmerId.slice(0, 8)}`}
                      >
                        <XCircle className="w-4 h-4" aria-hidden="true" />
                        Reject
                      </button>
                    </div>
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

export default DisbursementsPage;
