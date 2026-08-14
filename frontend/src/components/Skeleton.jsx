export function Skeleton({ className = '', width, height, variant = 'text' }) {
  return (
    <div
      className={`skeleton skeleton-${variant} ${className}`}
      style={{ width, height }}
      aria-hidden="true"
    />
  );
}

export function SkeletonTable({ rows = 5, cols = 4 }) {
  return (
    <div className="skeleton-table" aria-hidden="true">
      <div className="skeleton-table-header">
        {Array.from({ length: cols }, (_, i) => <Skeleton key={i} className="skeleton-th" />)}
      </div>
      {Array.from({ length: rows }, (_, r) => (
        <div className="skeleton-table-row" key={r}>
          {Array.from({ length: cols }, (_, c) => <Skeleton key={c} className="skeleton-td" />)}
        </div>
      ))}
    </div>
  );
}

export function SkeletonCard() {
  return (
    <div className="skeleton-card" aria-hidden="true">
      <Skeleton className="skeleton-card-icon" variant="circle" />
      <div className="skeleton-card-text">
        <Skeleton className="skeleton-card-label" />
        <Skeleton className="skeleton-card-value" />
      </div>
    </div>
  );
}

export function SkeletonChart() {
  return (
    <div className="skeleton-chart" aria-hidden="true">
      <Skeleton className="skeleton-chart-bar" />
      <Skeleton className="skeleton-chart-bar" style={{ height: '70%' }} />
      <Skeleton className="skeleton-chart-bar" style={{ height: '45%' }} />
      <Skeleton className="skeleton-chart-bar" style={{ height: '90%' }} />
      <Skeleton className="skeleton-chart-bar" style={{ height: '60%' }} />
      <Skeleton className="skeleton-chart-bar" style={{ height: '35%' }} />
      <Skeleton className="skeleton-chart-bar" style={{ height: '80%' }} />
    </div>
  );
}