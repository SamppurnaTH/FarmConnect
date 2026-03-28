import { useState, useEffect } from 'react';

/**
 * useCountdownState — Requirement 7.3, 7.4
 * Decrements every second. Fires `onExpire` when it reaches 0.
 * Uses setInterval at 1-second resolution, cleared on unmount.
 */
export function useCountdownState(
  expiresAt: string,
  onExpire?: () => void,
): number {
  const getRemaining = () =>
    Math.max(0, Math.floor((new Date(expiresAt).getTime() - Date.now()) / 1000));

  const [seconds, setSeconds] = useState(getRemaining);

  useEffect(() => {
    if (getRemaining() <= 0) {
      onExpire?.();
      return;
    }

    const interval = setInterval(() => {
      const rem = getRemaining();
      setSeconds(rem);
      if (rem <= 0) {
        clearInterval(interval);
        onExpire?.();
      }
    }, 1000);

    return () => clearInterval(interval);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [expiresAt]);

  return seconds;
}
