import React from 'react';

export const KpiSkeleton: React.FC = () => (
  <div className="card animate-pulse">
    <div className="h-4 bg-gray-200 rounded w-1/3 mb-3" />
    <div className="h-8 bg-gray-200 rounded w-2/3 mb-2" />
    <div className="h-3 bg-gray-100 rounded w-1/2" />
  </div>
);

export const LoadingSkeleton: React.FC<{ count?: number }> = ({ count = 4 }) => (
  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
    {Array.from({ length: count }).map((_, i) => (
      <KpiSkeleton key={i} />
    ))}
  </div>
);

export const TableSkeleton: React.FC<{ rows?: number; cols?: number }> = ({
  rows = 5,
  cols = 4,
}) => (
  <div className="animate-pulse">
    <div className="h-10 bg-gray-100 rounded mb-2" />
    {Array.from({ length: rows }).map((_, r) => (
      <div key={r} className="grid gap-4 py-3 border-b border-gray-100"
        style={{ gridTemplateColumns: `repeat(${cols}, 1fr)` }}>
        {Array.from({ length: cols }).map((_, c) => (
          <div key={c} className="h-4 bg-gray-200 rounded" />
        ))}
      </div>
    ))}
  </div>
);
