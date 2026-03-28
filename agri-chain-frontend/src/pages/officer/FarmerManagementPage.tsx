import React, { useEffect, useState } from 'react';
import { NavMenu } from '../../components/NavMenu';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { farmersApi } from '../../api/farmers';
import { useToast } from '../../components/ToastProvider';
import { useIsOnline } from '../../components/ConnectivityBanner';
import type { FarmerProfile, FarmerDocument, VerificationStatus } from '../../types';
import { ChevronDown, ChevronUp, Check, X } from 'lucide-react';

/**
 * FarmerManagementPage — Requirement 4.1-4.5
 * Market Officer and Administrator view.
 */
const FarmerManagementPage: React.FC = () => {
  const { showToast } = useToast();
  const isOnline = useIsOnline();
  const [farmers, setFarmers] = useState<FarmerProfile[]>([]);
  const [docs, setDocs] = useState<Record<string, FarmerDocument[]>>({});
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [rejectReason, setRejectReason] = useState<Record<string, string>>({});
  const [rejectError, setRejectError] = useState<Record<string, string>>({});

  useEffect(() => {
    farmersApi.listAll('Pending_Verification').then(setFarmers).finally(() => setLoading(false));
  }, []);

  const toggle = async (id: string) => {
    const next = expandedId === id ? null : id;
    setExpandedId(next);
    if (next && !docs[next]) {
      const d = await farmersApi.getDocuments(next);
      setDocs((p) => ({ ...p, [next]: d }));
    }
  };

  const handleVerify = async (farmerId: string, docId: string, status: VerificationStatus) => {
    const reason = rejectReason[docId];
    if (status === 'Rejected' && !reason?.trim()) {
      setRejectError((p) => ({ ...p, [docId]: 'Rejection reason is required.' }));
      return;
    }
    try {
      await farmersApi.verifyDocument(farmerId, docId, status, reason);
      const updated = await farmersApi.getDocuments(farmerId);
      setDocs((p) => ({ ...p, [farmerId]: updated }));
      showToast(`Document ${status.toLowerCase()}`, 'success');
    } catch { showToast('Action failed', 'error'); }
  };

  const handleApprove = async (farmerId: string) => {
    try {
      await farmersApi.updateStatus(farmerId, 'Active');
      setFarmers((prev) => prev.filter((f) => f.id !== farmerId));
      showToast('Farmer approved and activated', 'success');
    } catch { showToast('Approval failed', 'error'); }
  };

  if (loading) return <><NavMenu /><LoadingSpinner /></>;

  return (
    <div className="min-h-screen bg-gray-50">
      <NavMenu />
      <main className="max-w-4xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">Farmer Management</h1>
        <p className="text-gray-500 mb-4">Showing {farmers.length} farmer(s) pending verification</p>

        {farmers.length === 0 ? (
          <div className="card text-center py-12 text-gray-500">No farmers pending verification.</div>
        ) : (
          <div className="space-y-4">
            {farmers.map((f) => (
              <div key={f.id} className="bg-white rounded-xl border border-gray-200">
                <button
                  onClick={() => toggle(f.id)}
                  className="w-full flex items-center justify-between px-6 py-4 text-left"
                  aria-expanded={expandedId === f.id}
                  aria-label={`Toggle farmer ${f.name}`}
                >
                  <div>
                    <p className="font-semibold text-gray-900">{f.name}</p>
                    <p className="text-sm text-gray-500">{f.contactInfo}</p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="badge-pending">Pending Verification</span>
                    {expandedId === f.id ? <ChevronUp className="w-4 h-4 text-gray-400" /> : <ChevronDown className="w-4 h-4 text-gray-400" />}
                  </div>
                </button>

                {expandedId === f.id && (
                  <div className="px-6 pb-6 border-t border-gray-100">
                    <h3 className="font-medium text-gray-800 mt-4 mb-3">Documents</h3>
                    {(docs[f.id] ?? []).length === 0 ? (
                      <p className="text-sm text-gray-400">No documents uploaded</p>
                    ) : (
                      <div className="space-y-3">
                        {(docs[f.id] ?? []).map((doc) => (
                          <div key={doc.id} className="flex items-center justify-between bg-gray-50 rounded-lg px-4 py-3">
                            <div>
                              <p className="font-medium text-sm">{doc.documentType.replace(/_/g,' ')}</p>
                              <span className={doc.verificationStatus === 'Verified' ? 'badge-verified' : doc.verificationStatus === 'Rejected' ? 'badge-rejected' : 'badge-pending'}>
                                {doc.verificationStatus}
                              </span>
                            </div>
                            {doc.verificationStatus === 'Pending' && (
                              <div className="flex items-center gap-2">
                                <input
                                  className="input-field text-xs w-40"
                                  placeholder="Rejection reason"
                                  aria-label={`Rejection reason for ${doc.documentType}`}
                                  value={rejectReason[doc.id] ?? ''}
                                  onChange={(e) => setRejectReason((p) => ({ ...p, [doc.id]: e.target.value }))}
                                />
                                {rejectError[doc.id] && <p className="text-xs text-red-500">{rejectError[doc.id]}</p>}
                                <button
                                  onClick={() => handleVerify(f.id, doc.id, 'Verified')}
                                  className="p-2 bg-green-100 text-green-700 rounded-lg hover:bg-green-200"
                                  aria-label={`Verify ${doc.documentType}`}
                                  disabled={!isOnline}
                                >
                                  <Check className="w-4 h-4" />
                                </button>
                                <button
                                  onClick={() => handleVerify(f.id, doc.id, 'Rejected')}
                                  className="p-2 bg-red-100 text-red-700 rounded-lg hover:bg-red-200"
                                  aria-label={`Reject ${doc.documentType}`}
                                  disabled={!isOnline}
                                >
                                  <X className="w-4 h-4" />
                                </button>
                              </div>
                            )}
                          </div>
                        ))}
                      </div>
                    )}

                    <button
                      onClick={() => handleApprove(f.id)}
                      className="btn-primary mt-4"
                      disabled={!isOnline}
                      aria-label={`Approve registration for ${f.name}`}
                    >
                      Approve Registration
                    </button>
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

export default FarmerManagementPage;
