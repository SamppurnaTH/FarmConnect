import React, { useEffect, useState } from 'react';
import { NavMenu } from '../../components/NavMenu';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { FieldError } from '../../components/FieldError';
import { farmersApi } from '../../api/farmers';
import { useAuthStore } from '../../stores/authStore';
import { useToast } from '../../components/ToastProvider';
import { useIsOnline } from '../../components/ConnectivityBanner';
import type { FarmerProfile, FarmerDocument } from '../../types';
import { Upload, FileCheck, FileX, Clock, Info } from 'lucide-react';

const statusBadge = (s: string) => {
  if (s === 'Verified') return 'badge-verified';
  if (s === 'Rejected') return 'badge-rejected';
  return 'badge-pending';
};

const DOCUMENT_TYPES = ['National_ID', 'Land_Title', 'Tax_Certificate'] as const;
type DocType = typeof DOCUMENT_TYPES[number];

/**
 * FarmerProfilePage — Requirement 3.5, 3.6, 3.7
 *
 * Uses GET /farmers/me to load the profile — the JWT identifies the user,
 * so we never need to pass a userId in the URL.
 */
const FarmerProfilePage: React.FC = () => {
  const { userId } = useAuthStore();
  const { showToast } = useToast();
  const isOnline = useIsOnline();

  const [profile, setProfile] = useState<FarmerProfile | null>(null);
  const [docs, setDocs] = useState<FarmerDocument[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [editing, setEditing] = useState(false);
  const [editForm, setEditForm] = useState({
    name: '', address: '', contactInfo: '', landDetails: '',
  });
  const [contactChanged, setContactChanged] = useState(false);
  const [saving, setSaving] = useState(false);

  const [uploading, setUploading] = useState<DocType | null>(null);

  // ── Load profile and documents ────────────────────────────────────────────
  const loadProfile = async () => {
    setLoading(true);
    setLoadError(null);
    try {
      // GET /farmers/me — identified by JWT, not by userId in URL
      const p = await farmersApi.getMyProfile();
      setProfile(p);
      setEditForm({
        name:        p.name,
        address:     p.address,
        contactInfo: p.contactInfo,
        landDetails: p.landDetails,
      });

      // Load documents using the farmer profile ID
      const d = await farmersApi.getDocuments(p.id);
      setDocs(d);
    } catch {
      setLoadError('Failed to load your profile. Please refresh the page.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (userId) loadProfile();
  }, [userId]);

  // ── Profile update ────────────────────────────────────────────────────────
  const handleSave = async () => {
    if (!profile) return;
    setSaving(true);
    try {
      const updated = await farmersApi.updateProfile(profile.id, editForm);
      setProfile(updated);
      setEditing(false);
      setContactChanged(false);
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
        name:        profile.name,
        address:     profile.address,
        contactInfo: profile.contactInfo,
        landDetails: profile.landDetails,
      });
    }
    setEditing(false);
    setContactChanged(false);
  };

  // ── Document upload ───────────────────────────────────────────────────────
  const handleUpload = async (docType: DocType, file: File) => {
    if (!profile) return;
    setUploading(docType);
    try {
      // Send the actual file bytes to the backend
      await farmersApi.uploadDocument(profile.id, docType, file);
      const updated = await farmersApi.getDocuments(profile.id);
      setDocs(updated);
      showToast('Document uploaded — pending verification', 'success');
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { error?: string } } })?.response?.data?.error
        ?? 'Upload failed. Please try again.';
      showToast(msg, 'error');
    } finally {
      setUploading(null);
    }
  };

  // ── Render ────────────────────────────────────────────────────────────────
  if (loading) return <><NavMenu /><LoadingSpinner /></>;

  if (loadError) {
    return (
      <div className="min-h-screen bg-gray-50">
        <NavMenu />
        <main className="max-w-3xl mx-auto px-4 py-8">
          <div role="alert" className="card text-center py-12 text-red-600">
            {loadError}
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <NavMenu />
      <main className="max-w-3xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">My Profile</h1>

        {/* ── Profile card ── */}
        <div className="card mb-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-semibold text-gray-900">Personal Information</h2>
            {!editing && (
              <button onClick={() => setEditing(true)} className="btn-secondary text-sm">
                Edit
              </button>
            )}
          </div>

          {editing ? (
            <div className="space-y-3">
              {contactChanged && (
                <div className="flex items-start gap-2 p-3 bg-blue-50 border border-blue-200 rounded-lg text-blue-800 text-sm">
                  <Info className="w-4 h-4 shrink-0 mt-0.5" aria-hidden="true" />
                  <span>
                    Updating your contact information requires re-verification before it will be used for notifications.
                  </span>
                </div>
              )}

              {(
                [
                  { field: 'name',        label: 'Full Name' },
                  { field: 'address',     label: 'Address' },
                  { field: 'contactInfo', label: 'Contact Info (Phone)' },
                  { field: 'landDetails', label: 'Land Details' },
                ] as const
              ).map(({ field, label }) => (
                <div key={field}>
                  <label htmlFor={`edit-${field}`} className="block text-sm font-medium text-gray-700 mb-1">
                    {label}
                  </label>
                  <input
                    id={`edit-${field}`}
                    className="input-field"
                    value={editForm[field]}
                    onChange={(e) => {
                      if (field === 'contactInfo' && e.target.value !== profile?.contactInfo) {
                        setContactChanged(true);
                      }
                      setEditForm((prev) => ({ ...prev, [field]: e.target.value }));
                    }}
                  />
                </div>
              ))}

              <div className="flex gap-3 pt-2">
                <button
                  onClick={handleSave}
                  className="btn-primary"
                  disabled={saving || !isOnline}
                  aria-busy={saving}
                >
                  {saving ? 'Saving…' : 'Save'}
                </button>
                <button onClick={handleCancel} className="btn-secondary">
                  Cancel
                </button>
              </div>
            </div>
          ) : (
            <dl className="grid grid-cols-2 gap-x-6 gap-y-3 text-sm">
              {(
                [
                  ['Name',         profile?.name],
                  ['Date of Birth', profile?.dateOfBirth],
                  ['Gender',       profile?.gender],
                  ['Address',      profile?.address],
                  ['Contact Info', profile?.contactInfo],
                  ['Land Details', profile?.landDetails],
                  ['Status',       profile?.status],
                ] as [string, string | undefined][]
              ).map(([label, value]) => (
                <div key={label}>
                  <dt className="text-gray-500">{label}</dt>
                  <dd className="font-medium text-gray-900">{value || '—'}</dd>
                </div>
              ))}
            </dl>
          )}
        </div>

        {/* ── Documents card ── */}
        <div className="card">
          <h2 className="font-semibold text-gray-900 mb-4">KYC Documents</h2>
          <p className="text-sm text-gray-500 mb-4">
            Upload your identity documents for verification. Your account must be verified before you can create crop listings.
          </p>

          {DOCUMENT_TYPES.map((docType) => {
            const doc = docs.find((d) => d.documentType === docType);
            const isUploading = uploading === docType;

            return (
              <div
                key={docType}
                className="flex items-center justify-between py-3 border-b border-gray-100 last:border-0"
              >
                <div className="flex items-center gap-3">
                  {doc?.verificationStatus === 'Verified' ? (
                    <FileCheck className="w-5 h-5 text-green-500" aria-hidden="true" />
                  ) : doc?.verificationStatus === 'Rejected' ? (
                    <FileX className="w-5 h-5 text-red-500" aria-hidden="true" />
                  ) : (
                    <Clock className="w-5 h-5 text-gray-400" aria-hidden="true" />
                  )}

                  <div>
                    <p className="font-medium text-sm text-gray-900">
                      {docType.replace(/_/g, ' ')}
                    </p>
                    {doc ? (
                      <span className={statusBadge(doc.verificationStatus)}>
                        {doc.verificationStatus}
                      </span>
                    ) : (
                      <span className="text-xs text-gray-400">Not uploaded</span>
                    )}
                    {doc?.rejectionReason && (
                      <p className="text-xs text-red-600 mt-0.5">
                        Reason: {doc.rejectionReason}
                      </p>
                    )}
                    {doc && (
                      <a
                        href={farmersApi.getDocumentFileUrl(profile!.id, doc.id)}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-xs text-primary-600 hover:underline mt-0.5 block"
                        aria-label={`View uploaded ${docType.replace(/_/g, ' ')}`}
                      >
                        View uploaded file ↗
                      </a>
                    )}
                  </div>
                </div>

                <label
                  htmlFor={`upload-${docType}`}
                  className={`btn-secondary text-xs cursor-pointer flex items-center gap-1 ${isUploading ? 'opacity-60 pointer-events-none' : ''}`}
                  title="Accepted: PDF, JPG, PNG — max 10 MB"
                >
                  <Upload className="w-3 h-3" aria-hidden="true" />
                  {isUploading ? 'Uploading…' : doc ? 'Re-upload' : 'Upload'}
                  <input
                    id={`upload-${docType}`}
                    type="file"
                    className="sr-only"
                    accept=".pdf,.jpg,.jpeg,.png"
                    aria-label={`Upload ${docType.replace(/_/g, ' ')} (PDF, JPG or PNG, max 10 MB)`}
                    disabled={isUploading || !isOnline}
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      if (file) handleUpload(docType, file);
                      e.target.value = '';
                    }}
                  />
                </label>
              </div>
            );
          })}
        </div>
      </main>
    </div>
  );
};

export default FarmerProfilePage;
