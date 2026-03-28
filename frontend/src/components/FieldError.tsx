import React from 'react';

interface FieldErrorProps {
  message?: string;
}

/**
 * Renders a backend field-level validation error adjacent to its input.
 * Requirement 3.2, 14.3 — Property 8.
 */
export const FieldError: React.FC<FieldErrorProps> = ({ message }) => {
  if (!message) return null;
  return (
    <p className="mt-1 text-sm text-red-600" role="alert" aria-live="polite">
      {message}
    </p>
  );
};
