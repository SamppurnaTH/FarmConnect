import React, { useEffect, useState } from 'react';
import { NavMenu } from '../../components/NavMenu';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { subsidiesApi } from '../../api/subsidies';
import { useToast } from '../../components/ToastProvider';
import { useIsOnline } from '../../components/ConnectivityBanner';
import type { SubsidyProgram, Disbursement } from '../../types';
import { Plus, Play, Square, DollarSign } from 'lucide-react';

/**
 * SubsidyProgramsPage — Requirement 8.1-8.6
 */
const SubsidyProgramsPage: React.FC = () => {
  const { showToast } = useToast();
  const isOnline = useIsOnline();
  const [programs, setPrograms] = useState<SubsidyProgram[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [createForm, setCreateForm] = useState({ title: '', description: '', startDate: '', endDate: '', budgetAmount: '' });
  const [disbFormId, setDisbFormId] = useState<string | null>(null);
  const [disbForm, setDisbForm] = useState({ farmerId: '', amount: '', programCycle: '' });
  const [disbError, setDisbError] = useState<string | null>(null);

  const load = () => subsidiesApi.getPrograms().then(setPrograms).finally(() => setLoading(false));
  useEffect(() => { load(); }, []);

  const handleCreate = async () => {
    const budget = Number(createForm.budgetAmount);
    if (!createForm.title || !createForm.startDate || !createForm.endDate || budget <= 0) {
      showToast('All fields required', 'error');
      return;
    }
    try {
      await subsidiesApi.createProgram({ ...createForm, budgetAmount: budget });
      showToast('Program created', 'success');
      setShowCreate(false);
      setCreateForm({ title: '', description: '', startDate: '', endDate: '', budgetAmount: '' });
      load();
    } catch { showToast('Failed to create program', 'error'); }
  };

  const handleActivate = async (id: string) => {
    try {
      await subsidiesApi.activateProgram(id);
      setPrograms((p) => p.map((pr) => pr.id === id ? { ...pr, status: 'Active' } : pr));
      showToast('Program activated', 'success');
    } catch { showToast('Activation failed', 'error'); }
  };

  const handleClose = async (id: string) => {
    try {
      await subsidiesApi.closeProgram(id);
      setPrograms((p) => p.map((pr) => pr.id === id ? { ...pr, status: 'Closed' } : pr));
      showToast('Program closed', 'success');
    } catch { showToast('Failed to close program', 'error'); }
  };

  const handleDisbursement = async (programId: string) => {
    const amount = Number(disbForm.amount);
    if (!disbForm.farmerId || amount <= 0 || !disbForm.programCycle) {
      setDisbError('All fields are required.');
      return;
    }
    try {
      await subsidiesApi.createDisbursement(programId, { farmerId: disbForm.farmerId, amount, programCycle: disbForm.programCycle });
      showToast('Disbursement created', 'success');
      setDisbFormId(null);
      setDisbForm({ farmerId: '', amount: '', programCycle: '' });
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      if (status === 422) setDisbError('Disbursement amount exceeds remaining program budget.');
      else setDisbError('Failed to create disbursement.');
    }
  };

  const statusBadge: Record<string, string> = { Draft: 'badge-pending', Active: 'badge-active', Closed: 'badge-closed' };

  if (loading) return <><NavMenu /><LoadingSpinner /></>;

  return (
    <div className="min-h-screen bg-gray-50">
      <NavMenu />
      <main className="max-w-4xl mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold text-gray-900">Subsidy Programs</h1>
          <button className="btn-primary flex items-center gap-2" onClick={() => setShowCreate((s) => !s)}>
            <Plus className="w-4 h-4" /> New Program
          </button>
        </div>

        {showCreate && (
          <div className="card mb-6">
            <h2 className="font-semibold mb-4">Create Program</h2>
            <div className="grid grid-cols-2 gap-4">
              {[['title','Title','text'],['description','Description','text'],['startDate','Start Date','date'],['endDate','End Date','date'],['budgetAmount','Budget ($)','number']].map(([f,label,type]) => (
                <div key={f}>
                  <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
                  <input type={type} className="input-field" value={createForm[f as keyof typeof createForm]}
                    onChange={(e) => setCreateForm((p) => ({ ...p, [f]: e.target.value }))} />
                </div>
              ))}
            </div>
            <div className="flex gap-3 mt-4">
              <button onClick={handleCreate} className="btn-primary" disabled={!isOnline}>Create</button>
              <button onClick={() => setShowCreate(false)} className="btn-secondary">Cancel</button>
            </div>
          </div>
        )}

        <div className="space-y-4">
          {programs.map((p) => (
            <div key={p.id} className="card">
              <div className="flex items-start justify-between mb-3">
                <div>
                  <h3 className="font-semibold text-gray-900">{p.title}</h3>
                  <p className="text-sm text-gray-500 mt-0.5">{p.description}</p>
                  <p className="text-xs text-gray-400">{p.startDate} → {p.endDate}</p>
                </div>
                <span className={statusBadge[p.status]}>{p.status}</span>
              </div>

              <div className="grid grid-cols-3 gap-4 mb-4 text-sm">
                <div><p className="text-gray-500">Budget</p><p className="font-bold">${p.budgetAmount.toLocaleString()}</p></div>
                <div><p className="text-gray-500">Disbursed</p><p className="font-bold text-red-600">${p.totalDisbursed.toLocaleString()}</p></div>
                <div><p className="text-gray-500">Remaining</p><p className="font-bold text-green-600">${(p.budgetAmount - p.totalDisbursed).toLocaleString()}</p></div>
              </div>

              <div className="flex gap-3 flex-wrap">
                {p.status === 'Draft' && (
                  <button onClick={() => handleActivate(p.id)} className="btn-primary flex items-center gap-2" disabled={!isOnline} aria-label={`Activate ${p.title}`}>
                    <Play className="w-4 h-4" /> Activate
                  </button>
                )}
                {p.status === 'Active' && (
                  <>
                    <button onClick={() => handleClose(p.id)} className="btn-secondary flex items-center gap-2" disabled={!isOnline} aria-label={`Close ${p.title}`}>
                      <Square className="w-4 h-4" /> Close
                    </button>
                    <button onClick={() => setDisbFormId(disbFormId === p.id ? null : p.id)} className="btn-secondary flex items-center gap-2" aria-label={`Create disbursement for ${p.title}`}>
                      <DollarSign className="w-4 h-4" /> Add Disbursement
                    </button>
                  </>
                )}
              </div>

              {disbFormId === p.id && (
                <div className="mt-4 pt-4 border-t border-gray-100">
                  {disbError && <p className="text-sm text-red-600 mb-3">{disbError}</p>}
                  <div className="grid grid-cols-3 gap-3">
                    {[['farmerId','Farmer ID'],['amount','Amount ($)'],['programCycle','Program Cycle']].map(([f,label]) => (
                      <div key={f}>
                        <label className="block text-xs font-medium text-gray-600 mb-1">{label}</label>
                        <input className="input-field" type={f === 'amount' ? 'number' : 'text'}
                          value={disbForm[f as keyof typeof disbForm]}
                          onChange={(e) => setDisbForm((prev) => ({ ...prev, [f]: e.target.value }))} />
                      </div>
                    ))}
                  </div>
                  <button onClick={() => handleDisbursement(p.id)} className="btn-primary mt-3" disabled={!isOnline}>Submit Disbursement</button>
                </div>
              )}
            </div>
          ))}
        </div>
      </main>
    </div>
  );
};

export default SubsidyProgramsPage;
