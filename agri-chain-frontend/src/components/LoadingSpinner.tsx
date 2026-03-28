import React, { useEffect, useState } from 'react';

interface LoadingSpinnerProps {
  /** Delay in ms before showing spinner. Defaults to 500. */
  delay?: number;
  className?: string;
}

/**
 * Feature: agri-chain-frontend — Requirement 14.2
 * Displays a loading indicator after `delay` ms (default 500ms).
 * Cancelled immediately if the parent unmounts.
 */
export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
  delay = 500,
  className = '',
}) => {
  const [visible, setVisible] = useState(delay === 0);

  useEffect(() => {
    if (delay === 0) return;
    const timer = setTimeout(() => setVisible(true), delay);
    return () => clearTimeout(timer);
  }, [delay]);

  if (!visible) return null;

  return (
    <div
      className={`flex items-center justify-center p-8 ${className}`}
      role="status"
      aria-label="Loading"
    >
      <div className="w-8 h-8 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin" />
      <span className="sr-only">Loading…</span>
    </div>
  );
};
