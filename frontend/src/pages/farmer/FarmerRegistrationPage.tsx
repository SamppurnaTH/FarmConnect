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
  username:    z.string().min(3, 'Username must be at least 3 characters'),
  email:       z.string().email('Invalid email address'),
  password:    z
    .string()
    .min(8, 'Password must be at least 8 characters')
    .regex(/[A-Z]/, 'Password must contain at least one uppercase letter')
    .regex(/[0-9]/, 'Password must contain at least one number')
    .regex(/[^A-Za-z0-9]/, 'Password must contain at least one special character'),
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
        setServerError('This username or contact information is already registered. Please use different credentials.');
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
            Your account has been created and is <strong>pending verification</strong>. A Market Officer will review your profile. You'll receive a notification once activated.
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
          <h1 className="text-3xl font-bold text-gray-900">FarmConnect</h1>
          <p className="text-gray-500 mt-1">Join the FarmConnect platform</p>
        </div>

        <div className="card shadow-lg">
          {serverError && (
            <div role="alert" className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
              {serverError}
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-6">
            {/* --- Account Credentials Section --- */}
            <div className="space-y-4">
              <h3 className="text-sm font-semibold text-gray-400 uppercase tracking-wider">Account Credentials</h3>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label htmlFor="username" className="block text-sm font-medium text-gray-700 mb-1">Username</label>
                  <input
                    id="username"
                    type="text"
                    autoComplete="username"
                    className={`input-field ${errors.username ? 'border-red-400' : ''}`}
                    {...register('username')}
                  />
                  <FieldError message={errors.username?.message} />
                </div>
                <div>
                  <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">Email</label>
                  <input
                    id="email"
                    type="email"
                    autoComplete="email"
                    className={`input-field ${errors.email ? 'border-red-400' : ''}`}
                    {...register('email')}
                  />
                  <FieldError message={errors.email?.message} />
                </div>
              </div>

              <div>
                <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1">Password</label>
                <input
                  id="password"
                  type="password"
                  autoComplete="new-password"
                  className={`input-field ${errors.password ? 'border-red-400' : ''}`}
                  {...register('password')}
                />
                <FieldError message={errors.password?.message} />
              </div>
            </div>

            <hr className="border-gray-100" />

            {/* --- Farmer Profile Section --- */}
            <div className="space-y-4">
              <h3 className="text-sm font-semibold text-gray-400 uppercase tracking-wider">Farmer Profile</h3>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">Full Name</label>
                  <input
                    id="name"
                    type="text"
                    autoComplete="name"
                    className={`input-field ${errors.name ? 'border-red-400' : ''}`}
                    {...register('name')}
                  />
                  <FieldError message={errors.name?.message} />
                </div>
                <div>
                  <label htmlFor="dateOfBirth" className="block text-sm font-medium text-gray-700 mb-1">Date of Birth</label>
                  <input
                    id="dateOfBirth"
                    type="date"
                    className={`input-field ${errors.dateOfBirth ? 'border-red-400' : ''}`}
                    {...register('dateOfBirth')}
                  />
                  <FieldError message={errors.dateOfBirth?.message} />
                </div>
              </div>

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

              <div>
                <label htmlFor="contactInfo" className="block text-sm font-medium text-gray-700 mb-1">Contact Info (Phone)</label>
                <input
                  id="contactInfo"
                  type="text"
                  autoComplete="tel"
                  className={`input-field ${errors.contactInfo ? 'border-red-400' : ''}`}
                  {...register('contactInfo')}
                />
                <FieldError message={errors.contactInfo?.message} />
              </div>

              <div>
                <label htmlFor="address" className="block text-sm font-medium text-gray-700 mb-1">Address</label>
                <textarea
                  id="address"
                  rows={2}
                  className={`input-field ${errors.address ? 'border-red-400' : ''}`}
                  {...register('address')}
                />
                <FieldError message={errors.address?.message} />
              </div>

              <div>
                <label htmlFor="landDetails" className="block text-sm font-medium text-gray-700 mb-1">Land Details</label>
                <textarea
                  id="landDetails"
                  rows={2}
                  className={`input-field ${errors.landDetails ? 'border-red-400' : ''}`}
                  {...register('landDetails')}
                />
                <FieldError message={errors.landDetails?.message} />
              </div>
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

          <p className="mt-6 text-center text-sm text-gray-500">
            Already registered?{' '}
            <Link to="/login" className="text-primary-600 hover:underline">Sign in</Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default FarmerRegistrationPage;
