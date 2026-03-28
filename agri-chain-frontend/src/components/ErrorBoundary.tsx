import React from 'react';

interface ErrorBoundaryState {
  hasError: boolean;
  correlationId?: string;
}

/**
 * Catches render-time errors and displays a generic message.
 * If the error object contains a correlationId, it is shown.
 * Stack traces are never exposed to the user (Requirement 14.4).
 */
export class ErrorBoundary extends React.Component<
  { children: React.ReactNode },
  ErrorBoundaryState
> {
  state: ErrorBoundaryState = { hasError: false };

  static getDerivedStateFromError(error: unknown): ErrorBoundaryState {
    const correlationId =
      error instanceof Error
        ? (error as Error & { correlationId?: string }).correlationId
        : undefined;
    return { hasError: true, correlationId };
  }

  render() {
    if (this.state.hasError) {
      return (
        <div
          role="alert"
          className="min-h-screen flex items-center justify-center bg-gray-50"
        >
          <div className="card max-w-md text-center">
            <div className="text-red-500 text-5xl mb-4">⚠️</div>
            <h1 className="text-xl font-semibold text-gray-900 mb-2">
              Something went wrong
            </h1>
            <p className="text-gray-600 mb-4">
              An unexpected error occurred. Please refresh the page or contact support.
            </p>
            {this.state.correlationId && (
              <p className="text-xs text-gray-400 font-mono">
                Error reference: {this.state.correlationId}
              </p>
            )}
            <button
              className="btn-primary mt-4"
              onClick={() => window.location.reload()}
            >
              Reload page
            </button>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}
