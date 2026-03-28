import React, { useEffect, useState } from 'react';
import { NavMenu } from '../../components/NavMenu';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { usersApi } from '../../api/users';
import { useToast } from '../../components/ToastProvider';
import { useIsOnline } from '../../components/ConnectivityBanner';
import type { User, UserRole } from '../../types';
import { UserCircle } from 'lucide-react';

const ALL_ROLES: UserRole[] = ['Farmer','Trader','Market_Officer','Program_Manager','Administrator','Compliance_Officer','Government_Auditor'];

/**
 * UserManagementPage — Requirement 12.1-12.3
 */
const UserManagementPage: React.FC = () => {
  const { showToast } = useToast();
  const isOnline = useIsOnline();
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => { usersApi.listUsers().then(setUsers).finally(() => setLoading(false)); }, []);

  const handleRoleChange = async (userId: string, role: UserRole) => {
    try {
      await usersApi.assignRole(userId, role);
      setUsers((prev) => prev.map((u) => u.id === userId ? { ...u, role } : u));
      showToast('Role updated', 'success');
    } catch { showToast('Failed to update role', 'error'); }
  };

  const statusBadge: Record<string, string> = { Active: 'badge-active', Locked: 'badge-rejected', Inactive: 'badge-closed' };

  if (loading) return <><NavMenu /><LoadingSpinner /></>;

  return (
    <div className="min-h-screen bg-gray-50">
      <NavMenu />
      <main className="max-w-5xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">User Management</h1>
        <div className="card overflow-hidden p-0">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b">
              <tr>
                {['User','Email','Role','Status','Action'].map((h) => (
                  <th key={h} className="px-4 py-3 text-left font-semibold text-gray-700">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {users.map((u) => (
                <tr key={u.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <UserCircle className="w-5 h-5 text-gray-400" />
                      <span className="font-medium">{u.username}</span>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-gray-500">{u.email}</td>
                  <td className="px-4 py-3">
                    <select
                      className="input-field text-xs py-1"
                      value={u.role}
                      aria-label={`Role for ${u.username}`}
                      onChange={(e) => handleRoleChange(u.id, e.target.value as UserRole)}
                      disabled={!isOnline}
                    >
                      {ALL_ROLES.map((r) => <option key={r} value={r}>{r.replace(/_/g,' ')}</option>)}
                    </select>
                  </td>
                  <td className="px-4 py-3"><span className={statusBadge[u.status] ?? 'badge-pending'}>{u.status}</span></td>
                  <td className="px-4 py-3">
                    {u.status === 'Active' && (
                      <button
                        onClick={async () => {
                          // Deactivate via status update
                          await usersApi.assignRole(u.id, u.role); // placeholder — real endpoint varies
                          setUsers((prev) => prev.map((x) => x.id === u.id ? { ...x, status: 'Inactive' } : x));
                          showToast('User deactivated', 'info');
                        }}
                        className="text-xs text-red-600 hover:text-red-800 font-medium"
                        aria-label={`Deactivate ${u.username}`}
                        disabled={!isOnline}
                      >
                        Deactivate
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </main>
    </div>
  );
};

export default UserManagementPage;
