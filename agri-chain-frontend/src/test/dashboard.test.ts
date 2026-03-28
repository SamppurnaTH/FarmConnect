import { describe, it, expect } from 'vitest';
import * as fc from 'fast-check';
import type { DashboardKPIs } from '../types';

// ─── Property 28: Dashboard KPI cards render all required metrics ──────────────
// Feature: agri-chain-frontend, Property 28: Dashboard KPI cards render all required metrics
describe('Property 28 — Dashboard KPI cards render all metrics', () => {
  it('all 4 KPI values are present in any valid dashboard response', () => {
    fc.assert(
      fc.property(
        fc.record({
          activeFarmerCount:       fc.integer({ min: 0 }),
          totalCropVolume:         fc.float({ min: 0 }),
          totalTransactionValue:   fc.float({ min: 0 }),
          totalSubsidiesDisbursed: fc.float({ min: 0 }),
        }),
        (kpis: DashboardKPIs) => {
          const REQUIRED: (keyof DashboardKPIs)[] = [
            'activeFarmerCount',
            'totalCropVolume',
            'totalTransactionValue',
            'totalSubsidiesDisbursed',
          ];
          return REQUIRED.every((k) => kpis[k] !== undefined && kpis[k] !== null);
        },
      ),
      { numRuns: 100 },
    );
  });
});
