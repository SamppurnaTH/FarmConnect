import React from 'react';
import { AppRouter } from './router';
import { ErrorBoundary } from './components/ErrorBoundary';
import { ConnectivityBanner } from './components/ConnectivityBanner';
import { ToastProvider } from './components/ToastProvider';

const App: React.FC = () => {
  return (
    <ErrorBoundary>
      <ToastProvider>
        <ConnectivityBanner />
        <AppRouter />
      </ToastProvider>
    </ErrorBoundary>
  );
};

export default App;
