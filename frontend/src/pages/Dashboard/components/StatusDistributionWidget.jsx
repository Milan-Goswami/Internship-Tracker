import React from 'react';
import './UpcomingInterviewsWidget.css';

export const StatusDistributionWidget = ({ statusCounts = {} }) => {
  const entries = Object.entries(statusCounts);
  const total = entries.reduce((acc, [, count]) => acc + (count || 0), 0);

  return (
    <div className="dashboard-widget">
      <div className="widget-header">
        <span className="widget-title">Stage Distribution</span>
        <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-tertiary)' }}>
          {total} Total Applications
        </span>
      </div>

      {total === 0 ? (
        <div className="widget-empty">
          No applications tracked in pipeline yet.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          {entries.map(([status, count]) => {
            if (count === 0) return null;
            const pct = Math.round((count / total) * 100);

            return (
              <div key={status} style={{ display: 'flex', justifyContent: 'space-between', fontSize: 'var(--font-size-xs)' }}>
                <span style={{ color: 'var(--color-text-secondary)' }}>{status}</span>
                <span style={{ fontWeight: 'var(--font-weight-medium)', color: 'var(--color-text-primary)' }}>
                  {count} <span style={{ color: 'var(--color-text-tertiary)', fontWeight: 'normal' }}>({pct}%)</span>
                </span>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
