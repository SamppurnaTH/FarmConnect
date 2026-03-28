import React from 'react';
import { useCountdownState } from '../hooks/useCountdown';

interface CountdownTimerProps {
  expiresAt: string;
  onExpire?: () => void;
}

function formatTime(seconds: number): string {
  if (seconds <= 0) return '00:00:00';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  return [h, m, s].map((v) => String(v).padStart(2, '0')).join(':');
}

/**
 * CountdownTimer — Requirement 7.3, 7.4
 * Displays hours:minutes:seconds remaining until expiresAt.
 * When it reaches 0 it shows "Expired" and calls onExpire.
 */
const CountdownTimer: React.FC<CountdownTimerProps> = ({ expiresAt, onExpire }) => {
  const seconds = useCountdownState(expiresAt, onExpire);
  const expired = seconds <= 0;

  return (
    <span
      className={`font-mono font-medium tabular-nums ${
        expired ? 'text-red-600' : seconds < 3600 ? 'text-orange-600' : 'text-gray-700'
      }`}
      aria-label={expired ? 'Transaction expired' : `Time remaining: ${formatTime(seconds)}`}
    >
      {expired ? 'Expired' : formatTime(seconds)}
    </span>
  );
};

export default CountdownTimer;
