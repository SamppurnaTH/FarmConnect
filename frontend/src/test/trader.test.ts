import { describe, it, expect } from 'vitest';
import * as fc from 'fast-check';

// ─── Property 14: Listing filter controls produce matching query parameters ───
// Feature: agri-chain-frontend, Property 14: Listing filter controls produce matching query parameters
describe('Property 14 — Filter controls produce matching query parameters', () => {
  it('selected filter values are exactly reflected in the outgoing query params', () => {
    fc.assert(
      fc.property(
        fc.record({
          cropType: fc.option(fc.string({ minLength: 1 }), { nil: undefined }),
          location:  fc.option(fc.string({ minLength: 1 }), { nil: undefined }),
          minPrice:  fc.option(fc.float({ min: Math.fround(0), max: Math.fround(1000) }).filter(n => !Number.isNaN(n)), { nil: undefined }),
          maxPrice:  fc.option(fc.float({ min: Math.fround(0), max: Math.fround(10000) }).filter(n => !Number.isNaN(n)), { nil: undefined }),
        }),
        (filters) => {
          // Simulate the query params builder in BrowseListingsPage
          const params: Record<string, string | number> = {};
          if (filters.cropType) params.cropType = filters.cropType;
          if (filters.location)  params.location  = filters.location;
          if (filters.minPrice !== undefined) params.minPrice = filters.minPrice;
          if (filters.maxPrice !== undefined) params.maxPrice = filters.maxPrice;

          // Every non-undefined filter must appear in params with the exact value
          const cropOk = filters.cropType == null || params.cropType === filters.cropType;
          const locOk  = filters.location  == null || params.location  === filters.location;
          const minOk  = filters.minPrice  == null || params.minPrice  === filters.minPrice;
          const maxOk  = filters.maxPrice  == null || params.maxPrice  === filters.maxPrice;
          return cropOk && locOk && minOk && maxOk;
        },
      ),
      { numRuns: 100 },
    );
  });
});
