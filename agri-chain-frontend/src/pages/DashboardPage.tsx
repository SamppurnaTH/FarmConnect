import React, { useEffect, useState } from 'react';
import { NavMenu } from '../components/NavMenu';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { dashboardApi } from '../api/dashboard';
import { useAuthStore } from '../stores/authStore';
import type { DashboardKPIs } from '../types';
import { Users, ShoppingCart, DollarSign, Coins, RefreshCw } from 'lucide-react';

interface KpiCardProps {
  title: string;
  value: string;
  icon: React.ReactNode;
  color: string;
}

const KpiCard: React.FC<KpiCardProps> = ({ title, value, icon, color }) => (
  <div className="card flex items-center gap-4">
    <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${color}`}>
      {icon}
    </div>
    <div>
      <p className="text-sm text-gray-500">{title}</p>
      <p className="text-2xl font-bold text-gray-900 tabular-nums">{value}</p>
    </div>
  </div>
);

/**
 * DashboardPage — Requirement 10.1, 10.2
 * Shows 4 KPI cards loaded from GET /reporting/dashboard.
 */
const DashboardPage: React.FC = () => {
  const { role } = useAuthStore();
  const [kpis, setKpis] = useState<DashboardKPIs | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await dashboardApi.getKPIs();
      setKpis(data);
    } catch {
      setError('Failed to load dashboard data. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const fmt = (n?: number) =>
    n != null ? n.toLocaleString('en-US', { maximumFractionDigits: 2 }) : '—';

  return (
    <div className="min-h-screen bg-gray-50">
      <NavMenu />
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
            <p className="text-gray-500 mt-1">Platform overview for {role?.replace(/_/g, ' ')}</p>
          </div>
          <button
            onClick={load}
            className="btn-secondary flex items-center gap-2"
            aria-label="Refresh dashboard data"
          >
            <RefreshCw className="w-4 h-4" aria-hidden="true" />
            Refresh
          </button>
        </div>

        {loading ? (
          <LoadingSkeleton count={4} />
        ) : error ? (
          <div role="alert" className="card text-center py-12">
            <p className="text-red-600 mb-4">{error}</p>
            <button onClick={load} className="btn-primary">
              Retry
            </button>
          </div>
        ) : kpis ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6" data-testid="kpi-grid">
            <KpiCard
              title="Active Farmers"
              value={fmt(kpis.activeFarmerCount)}
              icon={<Users className="w-6 h-6 text-white" aria-hidden="true" />}
              color="bg-primary-500"
            />
            <KpiCard
              title="Total Crop Volume (kg)"
              value={fmt(kpis.totalCropVolume)}
              icon={<ShoppingCart className="w-6 h-6 text-white" aria-hidden="true" />}
              color="bg-agri-wheat"
            />
            <KpiCard
              title="Transaction Volume"
              value={`$${fmt(kpis.totalTransactionValue)}`}
              icon={<DollarSign className="w-6 h-6 text-white" aria-hidden="true" />}
              color="bg-agri-sky"
            />
            <KpiCard
              title="Subsidies Disbursed"
              value={`$${fmt(kpis.totalSubsidiesDisbursed)}`}
              icon={<Coins className="w-6 h-6 text-white" aria-hidden="true" />}
              color="bg-agri-earth"
            />
          </div>
        ) : null}
      </main>
    </div>
  );
};

export default DashboardPage;
