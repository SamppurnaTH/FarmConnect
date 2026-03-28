import React from 'react';
import { Link } from 'react-router-dom';
import { ShieldX } from 'lucide-react';

const ForbiddenPage: React.FC = () => (
  <div className="min-h-screen flex items-center justify-center bg-gray-50">
    <div className="card max-w-md text-center">
      <ShieldX className="w-16 h-16 text-red-400 mx-auto mb-4" />
      <h1 className="text-2xl font-bold text-gray-900 mb-2">Access Denied</h1>
      <p className="text-gray-600 mb-6">
        You don't have permission to access this page. If you believe this is an error, please contact your administrator.
      </p>
      <Link to="/dashboard" className="btn-primary" aria-label="Return to dashboard">
        Return to Dashboard
      </Link>
    </div>
  </div>
);

export default ForbiddenPage;
