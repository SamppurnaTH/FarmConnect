import React, { useEffect, useState } from 'react';
import { NavMenu } from '../../components/NavMenu';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { complianceApi } from '../../api/compliance';
import { useAuthStore } from '../../stores/authStore';
import { useToast } from '../../components/ToastProvider';
import { useIsOnline } from '../../components/ConnectivityBanner';
import type { ComplianceRecord } from '../../types';
import { Plus, CheckCircle, XCircle } from 'lucide-react';

/**
 * ComplianceRecordsPage — Requirement 9.1-9.2
 */
const ComplianceRecordsPage: React.FC = () => {
  const { role } = useAuthStore();
  const { showToast } = useToast();
  const isOnline = useIsOnline();
  const [records, setRecords] = useState<ComplianceRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState({ entityType: '', entityId: '' });
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState({ entityType: '', entityId: '', checkResult: 'Pass' as 'Pass'|'Fail', checkDate: '', notes: '' });

  const load = () =>
    complianceApi.getRecords(filters).then(setRecords).finally(() => setLoading(false));

  useEffect(() => { load(); }, [filters]);

  const handleCreate = async () => {
    if (!form.entityType || !form.entityId || !form.checkDate) {
      showToast('All fields required', 'error');
      return;
    }
    try {
      await complianceApi.createRecord(form);
      showToast('Record created', 'success');
      setShowCreate(false);
      load();
    } catch { showToast('Failed to create record', 'error'); }
  };

  if (loading) return <><NavMenu /><LoadingSpinner /></>;

  return (
    <div className="min-h-screen bg-gray-50">
      <NavMenu />
      <main className="max-w-5xl mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold text-gray-900">Compliance Records</h1>
          {role === 'Compliance_Officer' && (
            <button onClick={() => setShowCreate((s) => !s)} className="btn-primary flex items-center gap-2">
              <Plus className="w-4 h-4" /> New Record
            </button>
          )}
        </div>

        {/* Filters */}
        <div className="card mb-6 grid grid-cols-2 gap-4">
          {[['entityType','Entity Type'],['entityId','Entity ID']].map(([k,l]) => (
            <div key={k}>
              <label className="block text-xs font-medium text-gray-500 mb-1">{l}</label>
              <input className="input-field" value={filters[k as keyof typeof filters]}
                aria-label={l}
                onChange={(e) => setFilters((p) => ({ ...p, [k]: e.target.value }))} />
            </div>
          ))}
        </div>

        {showCreate && (
          <div className="card mb-6">
            <h2 className="font-semibold mb-4">New Compliance Check</h2>
            <div className="grid grid-cols-2 gap-4">
              {[['entityType','Entity Type','text'],['entityId','Entity ID','text'],['checkDate','Check Date','date'],['notes','Notes','text']].map(([f,label,type]) => (
                <div key={f}>
                  <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
                  <input type={type} className="input-field" value={form[f as keyof typeof form]}
                    onChange={(e) => setForm((p) => ({ ...p, [f]: e.target.value }))} />
                </div>
              ))}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Result</label>
                <select className="input-field" value={form.checkResult}
                  onChange={(e) => setForm((p) => ({ ...p, checkResult: e.target.value as 'Pass'|'Fail' }))}>
                  <option value="Pass">Pass</option>
                  <option value="Fail">Fail</option>
                </select>
              </div>
            </div>
            <div className="flex gap-3 mt-4">
              <button onClick={handleCreate} className="btn-primary" disabled={!isOnline}>Save</button>
              <button onClick={() => setShowCreate(false)} className="btn-secondary">Cancel</button>
            </div>
          </div>
        )}

        <div className="card overflow-hidden p-0">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b">
              <tr>{['Entity Type','Entity ID','Result','Date','Notes'].map((h) => <th key={h} className="px-4 py-3 text-left font-semibold text-gray-700">{h}</th>)}</tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {records.map((r) => (
                <tr key={r.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3">{r.entityType}</td>
                  <td className="px-4 py-3 font-mono text-xs">{r.entityId.slice(0,8)}…</td>
                  <td className="px-4 py-3">
                    <span className={r.checkResult === 'Pass' ? 'badge-active' : 'badge-rejected'}>
                      {r.checkResult === 'Pass' ? <><CheckCircle className="inline w-3 h-3 mr-1" />Pass</> : <><XCircle className="inline w-3 h-3 mr-1" />Fail</>}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-500">{r.checkDate}</td>
                  <td className="px-4 py-3 text-gray-500">{r.notes}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </main>
    </div>
  );
};

export default ComplianceRecordsPage;
