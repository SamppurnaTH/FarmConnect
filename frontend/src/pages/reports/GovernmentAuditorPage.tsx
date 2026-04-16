import React, { useEffect, useState } from 'react';
import { NavMenu } from '../../components/NavMenu';
import { LoadingSkeleton } from '../../components/LoadingSkeleton';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { dashboardApi } from '../../api/dashboard';
import { complianceApi } from '../../api/compliance';
import { subsidiesApi } from '../../api/subsidies';
import { reportsApi } from '../../api/reports';
import { useToast } from '../../components/ToastProvider';
import type { DashboardKPIs, Audit, SubsidyProgram, Disbursement, ReportMetadata } from '../../types';
import {
  Users, DollarSign, Coins, ShoppingCart,
  ClipboardCheck, FileText, Download, Loader2,
  BarChart3, RefreshCw,
} from 'lucide-react';

/**
 * GovernmentAuditorPage — unified read-only overview for the Government Auditor persona.
 * Surfaces KPIs, audit findings, subsidy programs, disbursements, and report history.
 */
const GovernmentAuditorPage: React.FC = () => {
  const { showToast } = useToast();

  const [kpis, setKpis] = useState<DashboardKPIs | null>(null);
  const [kpisLoading, setKpisLoading] = useState(true);

  const [audits, setAudits] = useState<Audit[]>([]);
  const [auditsLoading, setAuditsLoading] = useState(true);

  const [programs, setPrograms] = useState<SubsidyProgram[]>([]);
  const [disbursements, setDisbursements] = useState<Disbursement[]>([]);
  const [subsidyLoading, setSubsidyLoading] = useState(true);

  const [reports, setReports] = useState<ReportMetadata[]>([]);
  const [reportsLoading, setReportsLoading] = useState(true);

  const [exporting, setExporting] = useState<string | null>(null);

  const loadAll = () => {
    setKpisLoading(true);
    setAuditsLoading(true);
    setSubsidyLoading(true);
    setReportsLoading(true);

    dashboardApi.getKPIs()
      .then(setKpis)
      .catch(() => showToast('Failed to load KPIs', 'error'))
      .finally(() => setKpisLoading(false));

    complianceApi.getAudits()
      .then(setAudits)
      .catch(() => showToast('Failed to load audits', 'error'))
      .finally(() => setAuditsLoading(false));

    Promise.all([subsidiesApi.getPrograms(), subsidiesApi.getDisbursements()])
      .then(([p, d]) => { setPrograms(p); setDisbursements(d); })
      .catch(() => showToast('Failed to load subsidy data', 'error'))
      .finally(() => setSubsidyLoading(false));

    reportsApi.listReports()
      .then(setReports)
      .catch(() => showToast('Failed to load reports', 'error'))
      .finally(() => setReportsLoading(false));
  };

  useEffect(() => { loadAll(); }, []);

  const handleExport = async (id: string, format: string) => {
    setExporting(id);
    const controller = new AbortController();
    const timeout = setTimeout(() => {
      controller.abort();
      showToast('Export timed out. Please try again.', 'error');
      setExporting(null);
    }, 10_000);
    try {
      const blob = await reportsApi.exportReport(id, controller.signal);
      clearTimeout(timeout);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `report-${id}.${format.toLowerCase()}`;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      showToast('Export failed', 'error');
    } finally {
      setExporting(null);
      clearTimeout(timeout);
    }
  };

  const fmt = (n?: number) =>
    n != null ? n.toLocaleString('en-US', { maximumFractionDigits: 2 }) : '—';

  const completedAudits = audits.filter((a) => a.status === 'Completed');
  const inProgressAudits = audits.filter((a) => a.status === 'In_Progress');
  const activePrograms = programs.filter((p) => p.status === 'Active');
  const disbursedTotal = disbursements
    .filter((d) => d.status === 'Disbursed' || d.status === 'Approved')
    .reduce((sum, d) => sum + d.amount, 0);

  return (
    <div className="min-h-screen bg-gray-50">
      <NavMenu />
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Government Auditor Overview</h1>
            <p className="text-gray-500 mt-1">Read-only platform-wide audit view</p>
          </div>
          <button onClick={loadAll} className="btn-secondary flex items-center gap-2" aria-label="Refresh all data">
            <RefreshCw className="w-4 h-4" />
            Refresh
          </button>
        </div>

        {/* KPI Cards */}
        <section aria-label="Platform KPIs" className="mb-8">
          <h2 className="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
            <BarChart3 className="w-5 h-5 text-primary-600" /> Platform KPIs
          </h2>
          {kpisLoading ? (
            <LoadingSkeleton count={4} />
          ) : kpis ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              {[
                { title: 'Active Farmers',        value: fmt(kpis.activeFarmerCount),        icon: <Users className="w-5 h-5 text-white" />,       color: 'bg-primary-500' },
                { title: 'Total Crop Volume (kg)', value: fmt(kpis.totalCropVolume),          icon: <ShoppingCart className="w-5 h-5 text-white" />, color: 'bg-agri-wheat' },
                { title: 'Transaction Volume',     value: fmt(kpis.totalTransactionValue),    icon: <DollarSign className="w-5 h-5 text-white" />,   color: 'bg-agri-sky' },
                { title: 'Subsidies Disbursed',    value: fmt(kpis.totalSubsidiesDisbursed),  icon: <Coins className="w-5 h-5 text-white" />,        color: 'bg-agri-earth' },
              ].map(({ title, value, icon, color }) => (
                <div key={title} className="card flex items-center gap-4">
                  <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${color}`}>{icon}</div>
                  <div>
                    <p className="text-xs text-gray-500">{title}</p>
                    <p className="text-xl font-bold text-gray-900 tabular-nums">{value}</p>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-gray-400 text-sm">KPI data unavailable.</p>
          )}
        </section>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">

          {/* Audit Findings */}
          <section aria-label="Audit findings">
            <h2 className="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
              <ClipboardCheck className="w-5 h-5 text-primary-600" /> Audit Findings
              <span className="ml-auto text-xs text-gray-400 font-normal">
                {completedAudits.length} completed · {inProgressAudits.length} in progress
              </span>
            </h2>
            {auditsLoading ? <LoadingSpinner /> : audits.length === 0 ? (
              <div className="card text-center py-8 text-gray-400 text-sm">No audits found.</div>
            ) : (
              <div className="space-y-3 max-h-96 overflow-y-auto pr-1">
                {audits.map((a) => (
                  <div key={a.id} className="card">
                    <div className="flex items-start justify-between mb-2">
                      <div>
                        <p className="font-medium text-gray-900 text-sm">{a.scope}</p>
                        <p className="text-xs text-gray-400">
                          Started: {new Date(a.initiatedAt).toLocaleDateString()}
                          {a.completedAt && ` · Completed: ${new Date(a.completedAt).toLocaleDateString()}`}
                        </p>
                      </div>
                      <span className={a.status === 'Completed' ? 'badge-active' : 'badge-pending'}>
                        {a.status.replace(/_/g, ' ')}
                      </span>
                    </div>
                    {a.findings && (
                      <div className="bg-gray-50 rounded p-2 mt-2">
                        <p className="text-xs font-medium text-gray-500 mb-1">Findings</p>
                        <p className="text-xs text-gray-700 leading-relaxed">{a.findings}</p>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </section>

          {/* Subsidy Programs */}
          <section aria-label="Subsidy programs">
            <h2 className="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
              <Coins className="w-5 h-5 text-primary-600" /> Subsidy Programs
              <span className="ml-auto text-xs text-gray-400 font-normal">
                {activePrograms.length} active · disbursed: {fmt(disbursedTotal)}
              </span>
            </h2>
            {subsidyLoading ? <LoadingSpinner /> : programs.length === 0 ? (
              <div className="card text-center py-8 text-gray-400 text-sm">No programs found.</div>
            ) : (
              <div className="space-y-3 max-h-96 overflow-y-auto pr-1">
                {programs.map((p) => (
                  <div key={p.id} className="card">
                    <div className="flex items-start justify-between mb-1">
                      <p className="font-medium text-gray-900 text-sm">{p.title}</p>
                      <span className={
                        p.status === 'Active' ? 'badge-active' :
                        p.status === 'Closed' ? 'badge-rejected' : 'badge-pending'
                      }>
                        {p.status}
                      </span>
                    </div>
                    <p className="text-xs text-gray-500 mb-2">{p.description}</p>
                    <div className="flex gap-4 text-xs text-gray-500">
                      <span>Budget: <strong className="text-gray-700">{fmt(p.budgetAmount)}</strong></span>
                      <span>Disbursed: <strong className="text-gray-700">{fmt(p.totalDisbursed)}</strong></span>
                      <span>{p.startDate} → {p.endDate}</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>
        </div>

        {/* Disbursements Table */}
        <section aria-label="Disbursements" className="mb-8">
          <h2 className="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
            <DollarSign className="w-5 h-5 text-primary-600" /> Disbursements
            <span className="ml-auto text-xs text-gray-400 font-normal">{disbursements.length} total</span>
          </h2>
          {subsidyLoading ? <LoadingSpinner /> : disbursements.length === 0 ? (
            <div className="card text-center py-8 text-gray-400 text-sm">No disbursements found.</div>
          ) : (
            <div className="card overflow-hidden p-0">
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="bg-gray-50 border-b">
                    <tr>
                      {['Farmer ID', 'Program Cycle', 'Amount', 'Status', 'Approved At'].map((h) => (
                        <th key={h} className="px-4 py-3 text-left font-semibold text-gray-700 text-xs">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {disbursements.map((d) => (
                      <tr key={d.id} className="hover:bg-gray-50">
                        <td className="px-4 py-3 font-mono text-xs">{d.farmerId.slice(0, 8)}…</td>
                        <td className="px-4 py-3 text-xs">{d.programCycle}</td>
                        <td className="px-4 py-3 font-medium tabular-nums">{fmt(d.amount)}</td>
                        <td className="px-4 py-3">
                          <span className={
                            d.status === 'Disbursed' || d.status === 'Approved' ? 'badge-active' :
                            d.status === 'Rejected' || d.status === 'Failed' ? 'badge-rejected' : 'badge-pending'
                          }>
                            {d.status}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-xs text-gray-400">
                          {d.approvedAt ? new Date(d.approvedAt).toLocaleDateString() : '—'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </section>

        {/* Report History */}
        <section aria-label="Report history">
          <h2 className="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
            <FileText className="w-5 h-5 text-primary-600" /> Report History
            <span className="ml-auto text-xs text-gray-400 font-normal">{reports.length} reports</span>
          </h2>
          {reportsLoading ? <LoadingSpinner /> : reports.length === 0 ? (
            <div className="card text-center py-8 text-gray-400 text-sm">
              No reports generated yet. Use the <a href="/reports" className="text-primary-600 underline">Reports</a> page to generate one.
            </div>
          ) : (
            <div className="space-y-3">
              {reports.map((r) => (
                <div key={r.id} className="card flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <FileText className="w-5 h-5 text-primary-400" />
                    <div>
                      <p className="font-medium text-gray-900 text-sm">{r.scope}</p>
                      <p className="text-xs text-gray-400">
                        {new Date(r.generationTimestamp).toLocaleString()} · {r.format}
                      </p>
                    </div>
                  </div>
                  <button
                    onClick={() => handleExport(r.id, r.format)}
                    className="btn-secondary flex items-center gap-2 text-sm"
                    disabled={exporting === r.id}
                    aria-label={`Export ${r.scope} report`}
                  >
                    {exporting === r.id
                      ? <Loader2 className="w-4 h-4 animate-spin" />
                      : <Download className="w-4 h-4" />}
                    {exporting === r.id ? 'Exporting…' : 'Export'}
                  </button>
                </div>
              ))}
            </div>
          )}
        </section>

      </main>
    </div>
  );
};

export default GovernmentAuditorPage;
