import React, { useEffect, useState } from 'react';
import { NavMenu } from '../../components/NavMenu';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { complianceApi } from '../../api/compliance';
import { useAuthStore } from '../../stores/authStore';
import { useToast } from '../../components/ToastProvider';
import { useIsOnline } from '../../components/ConnectivityBanner';
import type { Audit } from '../../types';
import { Plus, Send, Download, Loader2 } from 'lucide-react';

/**
 * AuditsPage — Requirement 9.3-9.6
 */
const AuditsPage: React.FC = () => {
  const { role, userId } = useAuthStore();
  const { showToast } = useToast();
  const isOnline = useIsOnline();
  const [audits, setAudits] = useState<Audit[]>([]);
  const [loading, setLoading] = useState(true);
  const [scope, setScope] = useState('');
  const [findings, setFindings] = useState<Record<string, string>>({});
  const [exporting, setExporting] = useState<string | null>(null);

  const load = () => complianceApi.getAudits().then(setAudits).finally(() => setLoading(false));
  useEffect(() => { load(); }, []);

  const handleCreate = async () => {
    if (!scope.trim()) { showToast('Scope is required', 'error'); return; }
    try {
      await complianceApi.createAudit(scope, userId || 'system');
      showToast('Audit initiated', 'success');
      setScope('');
      load();
    } catch { showToast('Failed to create audit', 'error'); }
  };

  const handleSubmitFindings = async (id: string) => {
    const f = findings[id]?.trim();
    if (!f) { showToast('Findings are required', 'error'); return; }
    try {
      await complianceApi.submitFindings(id, f);
      setAudits((prev) => prev.map((a) => a.id === id ? { ...a, status: 'Completed', findings: f } : a));
      showToast('Findings submitted', 'success');
    } catch { showToast('Failed to submit findings', 'error'); }
  };

  const handleExport = async (id: string) => {
    setExporting(id);
    const controller = new AbortController();
    const timeout = setTimeout(() => {
      controller.abort();
      showToast('Export is taking too long. Please try again.', 'error');
      setExporting(null);
    }, 10_000);

    try {
      const blob = await complianceApi.exportAuditPdf(id, controller.signal);
      clearTimeout(timeout);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `audit-${id}.pdf`;
      a.click();
      URL.revokeObjectURL(url);
    } catch { showToast('Export failed', 'error'); }
    finally { setExporting(null); clearTimeout(timeout); }
  };

  if (loading) return <><NavMenu /><LoadingSpinner /></>;

  return (
    <div className="min-h-screen bg-gray-50">
      <NavMenu />
      <main className="max-w-4xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">Audits</h1>

        {role === 'Compliance_Officer' && (
          <div className="card mb-6">
            <h2 className="font-semibold mb-3">Initiate New Audit</h2>
            <div className="flex gap-3">
              <input className="input-field flex-1" placeholder="Audit scope" value={scope}
                aria-label="Audit scope"
                onChange={(e) => setScope(e.target.value)} />
              <button onClick={handleCreate} className="btn-primary flex items-center gap-2" disabled={!isOnline}>
                <Plus className="w-4 h-4" /> Initiate
              </button>
            </div>
          </div>
        )}

        <div className="space-y-4">
          {audits.map((a) => (
            <div key={a.id} className="card">
              <div className="flex items-start justify-between mb-3">
                <div>
                  <h3 className="font-semibold text-gray-900">{a.scope}</h3>
                  <p className="text-xs text-gray-400">Started: {new Date(a.initiatedAt).toLocaleDateString()}</p>
                </div>
                <span className={a.status === 'Completed' ? 'badge-active' : 'badge-pending'}>
                  {a.status.replace(/_/g,' ')}
                </span>
              </div>

              {a.findings && (
                <div className="bg-gray-50 rounded-lg p-3 mb-3">
                  <p className="text-xs font-medium text-gray-500 mb-1">Findings</p>
                  <p className="text-sm text-gray-700">{a.findings}</p>
                </div>
              )}

              <div className="flex gap-3 flex-wrap">
                {a.status === 'In_Progress' && role === 'Compliance_Officer' && (
                  <div className="flex gap-2 w-full">
                    <textarea
                      className="input-field flex-1 resize-none"
                      rows={2}
                      placeholder="Enter audit findings…"
                      aria-label={`Findings for audit ${a.scope}`}
                      value={findings[a.id] ?? ''}
                      onChange={(e) => setFindings((p) => ({ ...p, [a.id]: e.target.value }))}
                    />
                    <button onClick={() => handleSubmitFindings(a.id)} className="btn-primary flex items-center gap-2 self-start" disabled={!isOnline}
                      aria-label={`Submit findings for audit ${a.scope}`}>
                      <Send className="w-4 h-4" /> Submit Findings
                    </button>
                  </div>
                )}
                {a.status === 'Completed' && (
                  <button
                    onClick={() => handleExport(a.id)}
                    className="btn-secondary flex items-center gap-2"
                    disabled={exporting === a.id}
                    aria-label={`Export PDF for audit ${a.scope}`}
                  >
                    {exporting === a.id ? <Loader2 className="w-4 h-4 animate-spin" /> : <Download className="w-4 h-4" />}
                    {exporting === a.id ? 'Exporting…' : 'Export PDF'}
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      </main>
    </div>
  );
};

export default AuditsPage;
