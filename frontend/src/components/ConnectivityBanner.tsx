import React, { useEffect, useState } from 'react';
import { WifiOff } from 'lucide-react';

/**
 * Listens to browser offline/online events and renders a warning banner
 * that disables form submission while disconnected (Requirement 14.5).
 */
export const ConnectivityBanner: React.FC = () => {
  const [isOffline, setIsOffline] = useState(!navigator.onLine);

  useEffect(() => {
    const handleOffline = () => setIsOffline(true);
    const handleOnline  = () => setIsOffline(false);

    window.addEventListener('offline', handleOffline);
    window.addEventListener('online',  handleOnline);

    return () => {
      window.removeEventListener('offline', handleOffline);
      window.removeEventListener('online',  handleOnline);
    };
  }, []);

  if (!isOffline) return null;

  return (
    <div
      role="alert"
      aria-live="assertive"
      className="fixed top-0 left-0 right-0 z-50 bg-red-600 text-white text-sm text-center py-2 px-4 flex items-center justify-center gap-2"
    >
      <WifiOff className="w-4 h-4" aria-hidden="true" />
      <span>You are offline. Form submissions are disabled until connectivity is restored.</span>
    </div>
  );
};

/** Hook to check current online status (use to disable form submit buttons) */
export const useIsOnline = (): boolean => {
  const [online, setOnline] = useState(navigator.onLine);

  useEffect(() => {
    const on  = () => setOnline(true);
    const off = () => setOnline(false);
    window.addEventListener('online',  on);
    window.addEventListener('offline', off);
    return () => { window.removeEventListener('online', on); window.removeEventListener('offline', off); };
  }, []);

  return online;
};
