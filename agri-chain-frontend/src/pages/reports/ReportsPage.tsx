import React, { useEffect, useState } from 'react';
import { NavMenu } from '../../components/NavMenu';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { reportsApi } from '../../api/reports';
import { useToast } from '../../components/ToastProvider';
import { useIsOnline } from '../../components/ConnectivityBanner';
import { usePolling } from '../../hooks/usePolling';
import type { ReportMetadata } from '../../types';
import { FileText, Download, Loader2, BarChart3 } from 'lucide-react';

/**
 * ReportsPage — Requirement 10.3-10.6
 */
const ReportsPage: React.FC = () => {
  const { showToast } = useToast();
  const isOnline = useIsOnline();
  const [reports, setReports] = useState<ReportMetadata[]>([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [pendingId, setPendingId] = useState<string | null>(null);
  const [form, setForm] = useState({ scope: '', startDate: '', endDate: '', format: 'PDF' as 'CSV'|'PDF' });
  const [formError, setFormError] = useState<string | null>(null);
  const [exporting, setExporting] = useState<string | null>(null);

  const loadReports = () => reportsApi.listReports().then(setReports).finally(() => setLoading(false));
  useEffect(() => { loadReports(); }, []);

  const handleGenerate = async () => {
    if (!form.scope || !form.startDate || !form.endDate) {
      setFormError('All fields are required.');
      return;
    }
    const start = new Date(form.startDate);
    const end = new Date(form.endDate);
    const diffMonths = (end.getFullYear() - start.getFullYear()) * 12 + (end.getMonth() - start.getMonth());
    if (diffMonths > 12) {
      setFormError('Date range cannot exceed 12 months.');
      return;
    }
    setFormError(null);
    setGenerating(true);
    try {
      const id = await reportsApi.generateReport(form);
      setPendingId(id);
      showToast('Report generation started…', 'info');
    } catch { showToast('Failed to generate report', 'error'); }
    finally { setGenerating(false); }
  };

  // Poll until the report appears in the list (terminal: it's in the history)
  usePolling(
    async () => {
      if (!pendingId) return;
      const all = await reportsApi.listReports();
      const found = all.find((r) => r.id === pendingId);
      if (found) {
        setReports(all);
        setPendingId(null);
        showToast('Report is ready for download!', 'success');
      }
    },
    5_000,
    !!pendingId,
  );

  const handleExport = async (id: string) => {
    setExporting(id);
    const controller = new AbortController();
    const timeout = setTimeout(() => {
      controller.abort();
      showToast('Export is taking too long. Please try again.', 'error');
      setExporting(null);
    }, 10_000);
    try {
      const blob = await reportsApi.exportReport(id, controller.signal);
      clearTimeout(timeout);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a'); a.href = url; a.download = `report-${id}.${form.format.toLowerCase()}`; a.click();
      URL.revokeObjectURL(url);
    } catch { showToast('Export failed', 'error'); }
    finally { setExporting(null); clearTimeout(timeout); }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <NavMenu />
      <main className="max-w-4xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">Reports</h1>

        {/* Generate form */}
        <div className="card mb-6">
          <h2 className="font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <BarChart3 className="w-5 h-5 text-primary-600" /> Generate Report
          </h2>
          {formError && <div role="alert" className="mb-3 p-3 bg-red-50 border border-red-200 rounded text-red-700 text-sm">{formError}</div>}
          <div className="grid grid-cols-2 gap-4">
            <div className="col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">Scope</label>
              <input className="input-field" placeholder="e.g. Farmers, Transactions, Subsidies" value={form.scope}
                onChange={(e) => setForm((p) => ({ ...p, scope: e.target.value }))} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Start Date</label>
              <input type="date" className="input-field" value={form.startDate}
                onChange={(e) => setForm((p) => ({ ...p, startDate: e.target.value }))} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">End Date (max 12 months)</label>
              <input type="date" className="input-field" value={form.endDate}
                onChange={(e) => setForm((p) => ({ ...p, endDate: e.target.value }))} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Format</label>
              <select className="input-field" value={form.format} aria-label="Report format"
                onChange={(e) => setForm((p) => ({ ...p, format: e.target.value as 'CSV'|'PDF' }))}>
                <option value="PDF">PDF</option>
                <option value="CSV">CSV</option>
              </select>
            </div>
          </div>
          <button onClick={handleGenerate} className="btn-primary mt-4 flex items-center gap-2"
            disabled={generating || !!pendingId || !isOnline}>
            {generating && <Loader2 className="w-4 h-4 animate-spin" />}
            {generating ? 'Generating…' : pendingId ? 'Processing…' : 'Generate Report'}
          </button>
          {pendingId && (
            <div className="mt-3 flex items-center gap-2 text-yellow-700 text-sm">
              <Loader2 className="w-4 h-4 animate-spin" /> Waiting for report to complete…
            </div>
          )}
        </div>

        {/* Report history */}
        <h2 className="font-semibold text-gray-900 mb-4">Report History</h2>
        {loading ? <LoadingSpinner /> : reports.length === 0 ? (
          <div className="card text-center py-12 text-gray-500">No reports generated yet.</div>
        ) : (
          <div className="space-y-3">
            {reports.map((r) => (
              <div key={r.id} className="card flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <FileText className="w-5 h-5 text-primary-500" />
                  <div>
                    <p className="font-medium text-gray-900">{r.scope}</p>
                    <p className="text-xs text-gray-400">{new Date(r.generationTimestamp).toLocaleString()} · {r.format}</p>
                  </div>
                </div>
                <button onClick={() => handleExport(r.id)} className="btn-secondary flex items-center gap-2"
                  disabled={exporting === r.id} aria-label={`Export ${r.scope} report`}>
                  {exporting === r.id ? <Loader2 className="w-4 h-4 animate-spin" /> : <Download className="w-4 h-4" />}
                  Export
                </button>
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  );
};

export default ReportsPage;
