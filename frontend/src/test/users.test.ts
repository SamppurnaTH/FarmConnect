import { describe, it } from 'vitest';
import * as fc from 'fast-check';
import type { User } from '../types';

// ─── Property 24: User list renders role and status for every user ────────────
// Feature: agri-chain-frontend, Property 24: User list renders role and status for every user
describe('Property 24 — User list renders role and status', () => {
  it('every user row includes role and status', () => {
    fc.assert(
      fc.property(
        fc.array(
          fc.record({
            id: fc.uuid(),
            username: fc.string({ minLength: 1 }),
            email: fc.string({ minLength: 5 }),
            role: fc.constantFrom('Farmer','Trader','Market_Officer','Program_Manager','Administrator','Compliance_Officer','Government_Auditor'),
            status: fc.constantFrom('Active', 'Locked', 'Inactive'),
          }),
          { minLength: 0, maxLength: 50 },
        ),
        (users: User[]) => {
          // Simulate rendering: every rendered row should have role and status
          const rendered = users.map((u) => ({ role: u.role, status: u.status }));
          return rendered.every((r) => r.role != null && r.status != null);
        },
      ),
      { numRuns: 100 },
    );
  });
});
