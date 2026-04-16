import React, { useEffect, useState } from 'react';
import { NavMenu } from '../../components/NavMenu';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { tradersApi } from '../../api/traders';
import { useAuthStore } from '../../stores/authStore';
import { useToast } from '../../components/ToastProvider';
import { useIsOnline } from '../../components/ConnectivityBanner';
import type { TraderProfile } from '../../types';

/**
 * TraderProfilePage
 * Trader views and edits their own profile (name, organization, contact info).
 */
const TraderProfilePage: React.FC = () => {
  const { userId } = useAuthStore();
  const { showToast } = useToast();
  const isOnline = useIsOnline();

  const [profile, setProfile]   = useState<TraderProfile | null>(null);
  const [loading, setLoading]   = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [editing, setEditing]   = useState(false);
  const [saving, setSaving]     = useState(false);
  const [editForm, setEditForm] = useState({
    name: '', organization: '', contactInfo: '',
  });

  // ── Load profile ──────────────────────────────────────────────────────────
  useEffect(() => {
    if (!userId) return;
    tradersApi.getMyProfile()
      .then((p) => {
        setProfile(p);
        setEditForm({
          name:         p.name,
          organization: p.organization ?? '',
          contactInfo:  p.contactInfo,
        });
      })
      .catch(() => setLoadError('Failed to load your profile. Please refresh.'))
      .finally(() => setLoading(false));
  }, [userId]);

  // ── Save profile ──────────────────────────────────────────────────────────
  const handleSave = async () => {
    if (!profile) return;
    setSaving(true);
    try {
      const updated = await tradersApi.updateProfile(profile.id, {
        name:         editForm.name || undefined,
        organization: editForm.organization || undefined,
        contactInfo:  editForm.contactInfo || undefined,
      });
      setProfile(updated);
      setEditing(false);
      showToast('Profile updated successfully', 'success');
    } catch {
      showToast('Failed to update profile. Please try again.', 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    if (profile) {
      setEditForm({
        name:         profile.name,
        organization: profile.organization ?? '',
        contactInfo:  profile.contactInfo,
      });
    }
    setEditing(false);
  };

  // ── Render ────────────────────────────────────────────────────────────────
  if (loading) return <><NavMenu /><LoadingSpinner /></>;

  if (loadError) {
    return (
      <div className="min-h-screen bg-gray-50">
        <NavMenu />
        <main className="max-w-2xl mx-auto px-4 py-8">
          <div role="alert" className="card text-center py-12 text-red-600">{loadError}</div>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <NavMenu />
      <main className="max-w-2xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">My Profile</h1>

        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-semibold text-gray-900">Trader Information</h2>
            {!editing && (
              <button onClick={() => setEditing(true)} className="btn-secondary text-sm">
                Edit
              </button>
            )}
          </div>

          {editing ? (
            <div className="space-y-4">
              {([
                { field: 'name',         label: 'Full Name' },
                { field: 'organization', label: 'Organization' },
                { field: 'contactInfo',  label: 'Contact Info (Phone)' },
              ] as const).map(({ field, label }) => (
                <div key={field}>
                  <label htmlFor={`edit-${field}`} className="block text-sm font-medium text-gray-700 mb-1">
                    {label}
                  </label>
                  <input
                    id={`edit-${field}`}
                    className="input-field"
                    value={editForm[field]}
                    onChange={(e) => setEditForm((p) => ({ ...p, [field]: e.target.value }))}
                  />
                </div>
              ))}
              <div className="flex gap-3 pt-2">
                <button
                  onClick={handleSave}
                  className="btn-primary disabled:opacity-50"
                  disabled={saving || !isOnline}
                  aria-busy={saving}
                >
                  {saving ? 'Saving…' : 'Save'}
                </button>
                <button onClick={handleCancel} className="btn-secondary">Cancel</button>
              </div>
            </div>
          ) : (
            <dl className="grid grid-cols-2 gap-x-6 gap-y-3 text-sm">
              {([
                ['Name',         profile?.name],
                ['Organization', profile?.organization || '—'],
                ['Contact Info', profile?.contactInfo],
                ['Status',       profile?.status],
                ['Member Since', profile?.createdAt
                  ? new Date(profile.createdAt).toLocaleDateString() : '—'],
              ] as [string, string | undefined][]).map(([label, value]) => (
                <div key={label}>
                  <dt className="text-gray-500">{label}</dt>
                  <dd className="font-medium text-gray-900">{value || '—'}</dd>
                </div>
              ))}
            </dl>
          )}
        </div>
      </main>
    </div>
  );
};

export default TraderProfilePage;
