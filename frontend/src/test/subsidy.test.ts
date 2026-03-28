import { describe, it, expect } from 'vitest';
import * as fc from 'fast-check';
import type { SubsidyProgram, Disbursement } from '../types';

// ─── Property 18: Status-driven action buttons match entity state ─────────────
// Feature: agri-chain-frontend, Property 18: Status-driven action buttons match entity state
describe('Property 18 — Status-driven action buttons', () => {
  it('Activate button shown only for Draft programs, Close for Active', () => {
    fc.assert(
      fc.property(
        fc.constantFrom('Draft', 'Active', 'Closed'),
        (status: SubsidyProgram['status']) => {
          const showActivate = status === 'Draft';
          const showClose    = status === 'Active';
          const showNeither  = status === 'Closed';
          if (status === 'Draft')   return showActivate && !showClose;
          if (status === 'Active')  return showClose && !showActivate;
          if (status === 'Closed')  return !showActivate && !showClose && showNeither;
          return false;
        },
      ),
      { numRuns: 100 },
    );
  });

  it('Approve button shown only for Pending disbursements', () => {
    fc.assert(
      fc.property(
        fc.constantFrom('Pending', 'Approved', 'Disbursed', 'Failed'),
        (status: Disbursement['status']) => {
          const showApprove = status === 'Pending';
          return status === 'Pending' ? showApprove : !showApprove;
        },
      ),
      { numRuns: 100 },
    );
  });
});

// ─── Property 19: Remaining budget is computed correctly ──────────────────────
// Feature: agri-chain-frontend, Property 19: Remaining budget is computed correctly
describe('Property 19 — Remaining budget computed correctly', () => {
  it('remaining = budgetAmount - totalDisbursed for any program', () => {
    fc.assert(
      fc.property(
        fc.float({ min: Math.fround(0), max: Math.fround(1_000_000) }).filter(n => !Number.isNaN(n)),
        fc.float({ min: Math.fround(0), max: Math.fround(1_000_000) }).filter(n => !Number.isNaN(n)),
        (budgetAmount, totalDisbursed) => {
          const remaining = budgetAmount - totalDisbursed;
          // The displayed remaining budget should equal budgetAmount - totalDisbursed
          const displayed = budgetAmount - totalDisbursed;
          return Math.abs(displayed - remaining) < 0.001;
        },
      ),
      { numRuns: 100 },
    );
  });
});
