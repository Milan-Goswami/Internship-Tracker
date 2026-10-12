import React from 'react';
import '../AnalyticsPage.css';

const STATUS_LABELS = {
  SAVED: { label: 'Saved', color: '#94a3b8' },
  APPLIED: { label: 'Applied', color: '#3b82f6' },
  SCREENING: { label: 'Screening', color: '#8b5cf6' },
  INTERVIEWING: { label: 'Interviewing', color: '#f59e0b' },
  OFFER: { label: 'Offer', color: '#10b981' },
  ACCEPTED: { label: 'Accepted', color: '#059669' },
  REJECTED: { label: 'Rejected', color: '#ef4444' },
  WITHDRAWN: { label: 'Withdrawn', color: '#64748b' },
};

export const StatusBreakdownCard = ({ statusCounts = {}, totalApplications = 0 }) => {
  const entries = Object.entries(statusCounts);

  return (
    <div className="status-breakdown-card">
      <div className="status-breakdown-header">
        <div>
          <h2 className="status-breakdown-title">Pipeline Status Distribution</h2>
          <p className="status-breakdown-subtitle">
            Current distribution of all {totalApplications} tracked application records.
          </p>
        </div>
      </div>

      <div className="status-distribution-grid">
        {entries.map(([status, count]) => {
          const config = STATUS_LABELS[status] || { label: status, color: '#6366f1' };
          const pct = totalApplications > 0
            ? Math.round((count / totalApplications) * 100)
            : 0;

          return (
            <div key={status} className="status-item-card">
              <div className="status-item-top">
                <span className="status-dot" style={{ backgroundColor: config.color }} />
                <span className="status-item-name">{config.label}</span>
                <span className="status-item-pct">{pct}%</span>
              </div>
              <div className="status-item-count">{count}</div>
              <div className="status-progress-track">
                <div
                  className="status-progress-fill"
                  style={{ width: `${pct}%`, backgroundColor: config.color }}
                />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
