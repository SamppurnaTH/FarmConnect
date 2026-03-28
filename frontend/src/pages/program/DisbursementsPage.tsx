import React, { useEffect, useState } from 'react';
import { NavMenu } from '../../components/NavMenu';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { subsidiesApi } from '../../api/subsidies';
import { useToast } from '../../components/ToastProvider';
import { useIsOnline } from '../../components/ConnectivityBanner';
import type { Disbursement } from '../../types';
import { CheckCircle } from 'lucide-react';

/**
 * DisbursementsPage — Requirement 8.7
 */
const DisbursementsPage: React.FC = () => {
  const { showToast } = useToast();
  const isOnline = useIsOnline();
  const [disbursements, setDisbursements] = useState<Disbursement[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    subsidiesApi.getDisbursements().then(setDisbursements).finally(() => setLoading(false));
  }, []);

  const handleApprove = async (id: string) => {
    try {
      await subsidiesApi.approveDisbursement(id);
      setDisbursements((prev) => prev.map((d) => d.id === id ? { ...d, status: 'Approved' } : d));
      showToast('Disbursement approved', 'success');
    } catch { showToast('Approval failed', 'error'); }
  };

  const sc: Record<string, string> = { Pending: 'badge-pending', Approved: 'badge-active', Disbursed: 'badge-settled', Failed: 'badge-failed' };

  if (loading) return <><NavMenu /><LoadingSpinner /></>;

  return (
    <div className="min-h-screen bg-gray-50">
      <NavMenu />
      <main className="max-w-4xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">Disbursements</h1>
        {disbursements.length === 0 ? (
          <div className="card text-center py-12 text-gray-500">No disbursements found.</div>
        ) : (
          <div className="card overflow-hidden p-0">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b text-left">
                <tr>
                  {['Farmer ID','Program','Amount','Cycle','Status','Action'].map((h) => (
                    <th key={h} className="px-4 py-3 font-semibold text-gray-700">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {disbursements.map((d) => (
                  <tr key={d.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 font-mono text-xs">{d.farmerId.slice(0,8)}…</td>
                    <td className="px-4 py-3 font-mono text-xs">{d.programId.slice(0,8)}…</td>
                    <td className="px-4 py-3 font-medium">${d.amount.toLocaleString()}</td>
                    <td className="px-4 py-3">{d.programCycle}</td>
                    <td className="px-4 py-3"><span className={sc[d.status]}>{d.status}</span></td>
                    <td className="px-4 py-3">
                      {d.status === 'Pending' && (
                        <button
                          onClick={() => handleApprove(d.id)}
                          className="flex items-center gap-1 text-xs btn-primary py-1.5 px-3"
                          disabled={!isOnline}
                          aria-label={`Approve disbursement for farmer ${d.farmerId}`}
                        >
                          <CheckCircle className="w-3 h-3" /> Approve
                        </button>
                      )}
                    </td>
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

export default DisbursementsPage;
