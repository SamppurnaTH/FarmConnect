import React, { useState } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { Sprout, Loader2 } from 'lucide-react';
import { useAuthStore } from '../stores/authStore';
import { FieldError } from '../components/FieldError';
import { useIsOnline } from '../components/ConnectivityBanner';

const schema = z.object({
  username: z.string().min(1, 'Username is required'),
  password: z.string().min(1, 'Password is required'),
});
type FormData = z.infer<typeof schema>;

/**
 * LoginPage — Requirement 1.1, 1.2, 1.6
 */
const LoginPage: React.FC = () => {
  const { login } = useAuthStore();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const isOnline = useIsOnline();

  const [globalError, setGlobalError] = useState<string | null>(null);
  const [locked, setLocked] = useState(false);
  const [loading, setLoading] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const sessionExpired = params.get('reason') === 'session_expired';
  const unauthorized   = params.get('reason') === 'unauthorized';

  const onSubmit = async (data: FormData) => {
    setGlobalError(null);
    setLocked(false);
    setLoading(true);
    try {
      await login(data);

      // Honour ?next= redirect set by RouteGuard, otherwise use role-based default
      const next = params.get('next');
      if (next && next.startsWith('/') && !next.startsWith('//')) {
        navigate(next, { replace: true });
        return;
      }

      const { role } = useAuthStore.getState();
      let target = '/dashboard';
      switch (role) {
        case 'Farmer':             target = '/dashboard';       break;
        case 'Trader':             target = '/listings/browse'; break;
        case 'Compliance_Officer': target = '/compliance';      break;
        case 'Government_Auditor': target = '/reports';         break;
        case 'Administrator':      target = '/audit-log';       break;
      }
      navigate(target, { replace: true });
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      if (status === 423) {
        setLocked(true);
      } else {
        // Generic message — never reveal which field was wrong (Req 1.2)
        setGlobalError('Invalid credentials. Please check your username and password.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary-50 via-white to-primary-100 px-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-primary-600 rounded-2xl mb-4 shadow-lg">
            <Sprout className="w-8 h-8 text-white" aria-hidden="true" />
          </div>
          <h1 className="text-4xl font-extrabold text-transparent bg-clip-text bg-gradient-to-r from-primary-600 to-primary-400">FarmConnect</h1>
          <p className="text-gray-500 mt-1">Transparency from Seed to Sale</p>
        </div>

        <div className="card shadow-xl">
          <h2 className="text-xl font-semibold text-gray-900 mb-6">Sign in to your account</h2>

          {/* Session expired / unauthorized notices */}
          {sessionExpired && (
            <div role="alert" className="mb-4 p-3 bg-yellow-50 border border-yellow-200 rounded-lg text-yellow-800 text-sm">
              Your session has expired. Please sign in again.
            </div>
          )}
          {unauthorized && (
            <div role="alert" className="mb-4 p-3 bg-yellow-50 border border-yellow-200 rounded-lg text-yellow-800 text-sm">
              You have been signed out. Please sign in to continue.
            </div>
          )}

          {/* Account locked notice */}
          {locked && (
            <div role="alert" className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-800 text-sm">
              Your account has been locked due to too many failed attempts. Please check your registered email for instructions.
            </div>
          )}

          {/* Generic auth error */}
          {globalError && !locked && (
            <div role="alert" className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
              {globalError}
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} noValidate>
            <div className="mb-4">
              <label htmlFor="username" className="block text-sm font-medium text-gray-700 mb-1">
                Username
              </label>
              <input
                id="username"
                type="text"
                autoComplete="username"
                className={`input-field ${errors.username ? 'border-red-400 focus:ring-red-400' : ''}`}
                aria-describedby={errors.username ? 'username-error' : undefined}
                {...register('username')}
              />
              <FieldError message={errors.username?.message} />
            </div>

            <div className="mb-6">
              <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1">
                Password
              </label>
              <input
                id="password"
                type="password"
                autoComplete="current-password"
                className={`input-field ${errors.password ? 'border-red-400 focus:ring-red-400' : ''}`}
                aria-describedby={errors.password ? 'password-error' : undefined}
                {...register('password')}
              />
              <FieldError message={errors.password?.message} />
            </div>

            <button
              type="submit"
              className="btn-primary w-full flex items-center justify-center gap-2"
              disabled={loading || !isOnline}
              aria-busy={loading}
            >
              {loading && <Loader2 className="w-4 h-4 animate-spin" aria-hidden="true" />}
              {loading ? 'Signing in…' : 'Sign in'}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-gray-600">
            New farmer?{' '}
            <Link to="/register" className="text-primary-600 hover:text-primary-700 font-medium">
              Register here
            </Link>
          </p>
          <p className="mt-2 text-center text-sm text-gray-600">
            New trader?{' '}
            <Link to="/register/trader" className="text-primary-600 hover:text-primary-700 font-medium">
              Register as Trader
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
