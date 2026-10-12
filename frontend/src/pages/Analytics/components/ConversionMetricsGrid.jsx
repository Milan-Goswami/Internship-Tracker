import React from 'react';
import '../AnalyticsPage.css';

export const ConversionMetricsGrid = ({ funnel }) => {
  const metrics = [
    {
      label: 'Overall Conversion',
      value: `${funnel?.overallConversionRate ?? 0}%`,
      subtext: 'Applied \u2192 Final Offer',
      trend: funnel?.overallConversionRate > 5 ? 'positive' : 'neutral',
      icon: (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <polyline points="23 6 13.5 15.5 8.5 10.5 1 18" />
          <polyline points="17 6 23 6 23 12" />
        </svg>
      ),
      color: '#10b981',
    },
    {
      label: 'Application Rate',
      value: `${funnel?.appliedConversionRate ?? 0}%`,
      subtext: 'Saved \u2192 Applied',
      trend: 'neutral',
      icon: (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M22 2L11 13" />
          <polygon points="22 2 15 22 11 13 2 9 22 2" />
        </svg>
      ),
      color: '#3b82f6',
    },
    {
      label: 'Screening Rate',
      value: `${funnel?.screeningConversionRate ?? 0}%`,
      subtext: 'Applied \u2192 Screening/OA',
      trend: 'neutral',
      icon: (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <circle cx="12" cy="12" r="10" />
          <polyline points="12 6 12 12 14 14" />
        </svg>
      ),
      color: '#8b5cf6',
    },
    {
      label: 'Interview Conversion',
      value: `${funnel?.interviewingConversionRate ?? 0}%`,
      subtext: 'Screening \u2192 Interviewing',
      trend: 'neutral',
      icon: (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <rect x="3" y="4" width="18" height="18" rx="2" />
          <line x1="16" y1="2" x2="16" y2="6" />
          <line x1="8" y1="2" x2="8" y2="6" />
        </svg>
      ),
      color: '#f59e0b',
    },
    {
      label: 'Offer Conversion',
      value: `${funnel?.offerConversionRate ?? 0}%`,
      subtext: 'Interviewing \u2192 Offer',
      trend: funnel?.offerConversionRate > 20 ? 'positive' : 'neutral',
      icon: (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <circle cx="12" cy="8" r="7" />
          <polyline points="8.21 13.89 7 23 12 20 17 23 15.79 13.88" />
        </svg>
      ),
      color: '#06b6d4',
    },
  ];

  return (
    <div className="analytics-metrics-grid">
      {metrics.map((m) => (
        <div key={m.label} className="analytics-metric-card">
          <div className="metric-header">
            <span className="metric-label">{m.label}</span>
            <div className="metric-icon" style={{ color: m.color }}>
              {m.icon}
            </div>
          </div>
          <div className="metric-value-row">
            <span className="metric-value">{m.value}</span>
          </div>
          <span className="metric-subtext">{m.subtext}</span>
        </div>
      ))}
    </div>
  );
};
