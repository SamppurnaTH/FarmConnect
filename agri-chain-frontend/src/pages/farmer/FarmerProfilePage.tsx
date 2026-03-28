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
  if (s === 'Verified')  return 'badge-verified';
  if (s === 'Rejected')  return 'badge-rejected';
  return 'badge-pending';
};

/**
 * FarmerProfilePage — Requirement 3.5, 3.6, 3.7
 */
const FarmerProfilePage: React.FC = () => {
  const { userId } = useAuthStore();
  const { showToast } = useToast();
  const isOnline = useIsOnline();
  const [profile, setProfile] = useState<FarmerProfile | null>(null);
  const [docs, setDocs] = useState<FarmerDocument[]>([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [editForm, setEditForm] = useState({ name: '', address: '', contactInfo: '', landDetails: '' });
  const [contactChanged, setContactChanged] = useState(false);
  const [uploading, setUploading] = useState<string | null>(null);

  useEffect(() => {
    if (!userId) return;
    Promise.all([
      farmersApi.getProfile(userId),
      farmersApi.getDocuments(userId),
    ]).then(([p, d]) => {
      setProfile(p);
      setDocs(d);
      setEditForm({ name: p.name, address: p.address, contactInfo: p.contactInfo, landDetails: p.landDetails });
    }).finally(() => setLoading(false));
  }, [userId]);

  const handleSave = async () => {
    if (!userId || !profile) return;
    try {
      await farmersApi.updateProfile(userId, editForm);
      setProfile((prev) => prev ? { ...prev, ...editForm } : prev);
      setEditing(false);
      showToast('Profile updated successfully', 'success');
      if (contactChanged) setContactChanged(false);
    } catch {
      showToast('Failed to update profile', 'error');
    }
  };

  const handleUpload = async (docType: string, file: File) => {
    if (!userId) return;
    setUploading(docType);
    const fd = new FormData();
    fd.append('file', file);
    fd.append('documentType', docType);
    try {
      await farmersApi.uploadDocument(userId, fd);
      const updated = await farmersApi.getDocuments(userId);
      setDocs(updated);
      showToast('Document uploaded — pending verification', 'success');
    } catch {
      showToast('Upload failed', 'error');
    } finally {
      setUploading(null);
    }
  };

  if (loading) return <><NavMenu /><LoadingSpinner /></>;

  return (
    <div className="min-h-screen bg-gray-50">
      <NavMenu />
      <main className="max-w-3xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">My Profile</h1>

        {/* Profile card */}
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
                  <Info className="w-4 h-4 shrink-0 mt-0.5" />
                  <span>Updating your contact information requires re-verification before it will be used for notifications.</span>
                </div>
              )}
              {(['name','address','contactInfo','landDetails'] as const).map((f) => (
                <div key={f}>
                  <label className="block text-sm font-medium text-gray-700 mb-1 capitalize">{f.replace(/([A-Z])/g,' $1')}</label>
                  <input
                    className="input-field"
                    value={editForm[f]}
                    onChange={(e) => {
                      if (f === 'contactInfo' && e.target.value !== profile?.contactInfo) setContactChanged(true);
                      setEditForm((prev) => ({ ...prev, [f]: e.target.value }));
                    }}
                  />
                </div>
              ))}
              <div className="flex gap-3 pt-2">
                <button onClick={handleSave} className="btn-primary" disabled={!isOnline}>Save</button>
                <button onClick={() => setEditing(false)} className="btn-secondary">Cancel</button>
              </div>
            </div>
          ) : (
            <dl className="grid grid-cols-2 gap-x-6 gap-y-3 text-sm">
              {[
                ['Name', profile?.name],
                ['Date of Birth', profile?.dateOfBirth],
                ['Gender', profile?.gender],
                ['Address', profile?.address],
                ['Contact Info', profile?.contactInfo],
                ['Land Details', profile?.landDetails],
                ['Status', profile?.status],
              ].map(([label, value]) => (
                <div key={label}>
                  <dt className="text-gray-500">{label}</dt>
                  <dd className="font-medium text-gray-900">{value || '—'}</dd>
                </div>
              ))}
            </dl>
          )}
        </div>

        {/* Documents */}
        <div className="card">
          <h2 className="font-semibold text-gray-900 mb-4">Documents</h2>
          {(['National_ID','Land_Title','Tax_Certificate'] as const).map((docType) => {
            const doc = docs.find((d) => d.documentType === docType);
            return (
              <div key={docType} className="flex items-center justify-between py-3 border-b border-gray-100 last:border-0">
                <div className="flex items-center gap-3">
                  {doc?.verificationStatus === 'Verified' ? <FileCheck className="w-5 h-5 text-green-500" /> :
                   doc?.verificationStatus === 'Rejected'  ? <FileX className="w-5 h-5 text-red-500" /> :
                   <Clock className="w-5 h-5 text-gray-400" />}
                  <div>
                    <p className="font-medium text-sm text-gray-900">{docType.replace(/_/g,' ')}</p>
                    {doc && (
                      <span className={statusBadge(doc.verificationStatus)}>
                        {doc.verificationStatus}
                      </span>
                    )}
                    {doc?.rejectionReason && (
                      <p className="text-xs text-red-600 mt-0.5">Reason: {doc.rejectionReason}</p>
                    )}
                  </div>
                </div>
                <label
                  htmlFor={`upload-${docType}`}
                  className="btn-secondary text-xs cursor-pointer flex items-center gap-1"
                >
                  <Upload className="w-3 h-3" />
                  {uploading === docType ? 'Uploading…' : doc ? 'Re-upload' : 'Upload'}
                  <input
                    id={`upload-${docType}`}
                    type="file"
                    className="sr-only"
                    accept=".pdf,.jpg,.jpeg,.png"
                    aria-label={`Upload ${docType.replace(/_/g,' ')}`}
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      if (file) handleUpload(docType, file);
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
