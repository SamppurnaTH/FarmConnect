import React from 'react';
import { Link } from 'react-router-dom';
import { SearchX } from 'lucide-react';

const NotFoundPage: React.FC = () => (
  <div className="min-h-screen flex items-center justify-center bg-gray-50">
    <div className="card max-w-md text-center">
      <SearchX className="w-16 h-16 text-gray-300 mx-auto mb-4" />
      <h1 className="text-2xl font-bold text-gray-900 mb-2">Page Not Found</h1>
      <p className="text-gray-600 mb-6">The page you're looking for doesn't exist.</p>
      <Link to="/dashboard" className="btn-primary" aria-label="Return to dashboard">
        Return to Dashboard
      </Link>
    </div>
  </div>
);

export default NotFoundPage;
