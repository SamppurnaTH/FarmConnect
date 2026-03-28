import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { Sprout, CheckCircle, Loader2 } from 'lucide-react';
import { farmersApi } from '../../api/farmers';
import { FieldError } from '../../components/FieldError';
import { useIsOnline } from '../../components/ConnectivityBanner';
import type { ApiErrorResponse } from '../../types';

const schema = z.object({
  name:        z.string().min(2, 'Full name is required'),
  dateOfBirth: z.string().min(1, 'Date of birth is required'),
  gender:      z.string().min(1, 'Gender is required'),
  address:     z.string().min(5, 'Address is required'),
  contactInfo: z.string().min(6, 'Contact information is required'),
  landDetails: z.string().min(3, 'Land details are required'),
});
type FormData = z.infer<typeof schema>;

/**
 * FarmerRegistrationPage — Requirement 3.1-3.4
 * Public registration form.
 */
const FarmerRegistrationPage: React.FC = () => {
  const isOnline = useIsOnline();
  const [success, setSuccess] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const {
    register, handleSubmit, setError,
    formState: { errors },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const onSubmit = async (data: FormData) => {
    setLoading(true);
    setServerError(null);
    try {
      await farmersApi.register(data);
      setSuccess(true);
    } catch (err: unknown) {
      const apiErr = (err as { response?: { data?: ApiErrorResponse; status?: number } })?.response;
      if (apiErr?.status === 409) {
        setServerError('This contact information is already registered. Please use a different contact.');
      } else if (apiErr?.data?.fields) {
        apiErr.data.fields.forEach((f) => {
          setError(f.field as keyof FormData, { message: f.message });
        });
        setServerError('Please correct the highlighted fields.');
      } else {
        setServerError('Registration failed. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary-50 to-white px-4">
        <div className="card max-w-md text-center">
          <CheckCircle className="w-16 h-16 text-primary-500 mx-auto mb-4" />
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Registration Submitted</h2>
          <p className="text-gray-600 mb-6">
            Your registration is <strong>pending verification</strong>. A Market Officer will review your documents. You'll receive a notification once your account is activated.
          </p>
          <Link to="/login" className="btn-primary">Back to Login</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary-50 via-white to-primary-100 px-4 py-12">
      <div className="max-w-xl mx-auto">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 bg-primary-600 rounded-2xl mb-4 shadow-lg">
            <Sprout className="w-7 h-7 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900">Farmer Registration</h1>
          <p className="text-gray-500 mt-1">Join the Agri-Chain platform</p>
        </div>

        <div className="card shadow-lg">
          {serverError && (
            <div role="alert" className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
              {serverError}
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
            {([
              ['name',        'Full Name',          'text',   'name'],
              ['dateOfBirth', 'Date of Birth',       'date',   undefined],
              ['address',     'Address',             'text',   'street-address'],
              ['contactInfo', 'Contact Info (phone/email)', 'text', 'tel'],
              ['landDetails', 'Land Details',        'text',   undefined],
            ] as [keyof FormData, string, string, string | undefined][]).map(([field, label, type, autoComplete]) => (
              <div key={field}>
                <label htmlFor={field} className="block text-sm font-medium text-gray-700 mb-1">
                  {label}
                </label>
                <input
                  id={field}
                  type={type}
                  autoComplete={autoComplete}
                  className={`input-field ${errors[field] ? 'border-red-400' : ''}`}
                  {...register(field)}
                />
                <FieldError message={errors[field]?.message} />
              </div>
            ))}

            <div>
              <label htmlFor="gender" className="block text-sm font-medium text-gray-700 mb-1">Gender</label>
              <select id="gender" className={`input-field ${errors.gender ? 'border-red-400' : ''}`} {...register('gender')}>
                <option value="">Select gender</option>
                <option value="Male">Male</option>
                <option value="Female">Female</option>
                <option value="Other">Other</option>
              </select>
              <FieldError message={errors.gender?.message} />
            </div>

            <button
              type="submit"
              className="btn-primary w-full flex items-center justify-center gap-2 mt-2"
              disabled={loading || !isOnline}
            >
              {loading && <Loader2 className="w-4 h-4 animate-spin" />}
              {loading ? 'Submitting…' : 'Submit Registration'}
            </button>
          </form>

          <p className="mt-4 text-center text-sm text-gray-500">
            Already registered?{' '}
            <Link to="/login" className="text-primary-600 hover:underline">Sign in</Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default FarmerRegistrationPage;
