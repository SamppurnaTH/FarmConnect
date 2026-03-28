import React from 'react';
import { X } from 'lucide-react';
import { useToastStore } from '../stores/toastStore';

export const useToast = () => {
  const showToast = useToastStore((state) => state.showToast);
  return { showToast };
};

export const ToastProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { toasts, dismissToast } = useToastStore();

  const colorMap = {
    success: 'bg-green-600',
    error:   'bg-red-600',
    info:    'bg-gray-800',
  };

  return (
    <>
      {children}
      <div
        aria-live="polite"
        aria-atomic="false"
        className="fixed bottom-4 right-4 z-50 flex flex-col gap-2 pointer-events-none"
      >
        {toasts.map((t) => (
          <div
            key={t.id}
            role="status"
            className={`flex items-start gap-3 pointer-events-auto text-white text-sm px-4 py-3 rounded-lg shadow-lg max-w-sm ${colorMap[t.type]} animate-in slide-in-from-right duration-200`}
          >
            <span className="flex-1">{t.message}</span>
            <button
              onClick={() => dismissToast(t.id)}
              aria-label="Dismiss notification"
              className="opacity-70 hover:opacity-100 shrink-0"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        ))}
      </div>
    </>
  );
};
