import { useEffect, useRef } from 'react';

/**
 * usePolling — Requirement 7.5, 11.4
 * Calls `fn` repeatedly at `intervalMs` while `active` is true.
 * Clears the interval on unmount or when `active` becomes false.
 */
export function usePolling(
  fn: () => void | Promise<void>,
  intervalMs: number,
  active: boolean = true,
): void {
  const fnRef = useRef(fn);
  fnRef.current = fn;

  useEffect(() => {
    if (!active) return;

    // Run immediately on mount
    fnRef.current();

    const id = setInterval(() => {
      fnRef.current();
    }, intervalMs);

    return () => clearInterval(id);
  }, [active, intervalMs]);
}
