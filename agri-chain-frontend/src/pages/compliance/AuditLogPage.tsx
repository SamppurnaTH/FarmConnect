import React, { useEffect, useState } from 'react';
import { NavMenu } from '../../components/NavMenu';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { complianceApi } from '../../api/compliance';
import type { AuditLog, Page } from '../../types';
import { Filter } from 'lucide-react';

/**
 * AuditLogPage — Requirement 12.4
 * Paginated audit log with date/action/resource filters, 50 per page.
 */
const AuditLogPage: React.FC = () => {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [filters, setFilters] = useState({ startDate: '', endDate: '', actionType: '', resourceType: '' });

  const load = async () => {
    setLoading(true);
    try {
      const result: Page<AuditLog> = await complianceApi.getAuditLog({ ...filters, page, size: 50 });
      setLogs(result.content);
      setTotalPages(result.totalPages);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [page, filters]);

  return (
    <div className="min-h-screen bg-gray-50">
      <NavMenu />
      <main className="max-w-6xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">Audit Log</h1>

        {/* Filters */}
        <div className="card mb-6">
          <div className="flex items-center gap-2 mb-3">
            <Filter className="w-4 h-4 text-gray-500" />
            <h2 className="font-medium text-gray-700">Filters</h2>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {[['startDate','Start Date','date'],['endDate','End Date','date'],['actionType','Action Type','text'],['resourceType','Resource Type','text']].map(([k,l,t]) => (
              <div key={k}>
                <label className="block text-xs font-medium text-gray-500 mb-1">{l}</label>
                <input type={t} className="input-field text-sm" aria-label={l}
                  value={filters[k as keyof typeof filters]}
                  onChange={(e) => { setFilters((p) => ({ ...p, [k]: e.target.value })); setPage(0); }} />
              </div>
            ))}
          </div>
        </div>

        {loading ? <LoadingSpinner /> : (
          <>
            <div className="card overflow-hidden p-0">
              <table className="w-full text-sm">
                <thead className="bg-gray-50 border-b">
                  <tr>
                    {['Timestamp','User','Action','Resource Type','Resource ID'].map((h) => (
                      <th key={h} className="px-4 py-3 text-left font-semibold text-gray-700">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {logs.map((l) => (
                    <tr key={l.id} className="hover:bg-gray-50">
                      <td className="px-4 py-3 text-xs text-gray-500 whitespace-nowrap">{new Date(l.timestamp).toLocaleString()}</td>
                      <td className="px-4 py-3 font-mono text-xs">{l.userId.slice(0,8)}…</td>
                      <td className="px-4 py-3 font-medium">{l.action}</td>
                      <td className="px-4 py-3 text-gray-600">{l.resourceType}</td>
                      <td className="px-4 py-3 font-mono text-xs">{l.resourceId.slice(0,8)}…</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            <div className="flex items-center justify-between mt-4">
              <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0}
                className="btn-secondary" aria-label="Previous page">← Prev</button>
              <span className="text-sm text-gray-500">Page {page + 1} of {totalPages}</span>
              <button onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}
                className="btn-secondary" aria-label="Next page">Next →</button>
            </div>
          </>
        )}
      </main>
    </div>
  );
};

export default AuditLogPage;
