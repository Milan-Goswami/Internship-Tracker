import React from 'react';
import '../AnalyticsPage.css';

const STAGE_CONFIG = {
  SAVED: {
    label: 'Saved Opportunities',
    color: '#94a3b8',
    icon: (
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z" />
      </svg>
    ),
  },
  APPLIED: {
    label: 'Submitted Applications',
    color: '#3b82f6',
    icon: (
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <path d="M22 2L11 13" />
        <polygon points="22 2 15 22 11 13 2 9 22 2" />
      </svg>
    ),
  },
  SCREENING: {
    label: 'Screening & Assessments',
    color: '#8b5cf6',
    icon: (
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <circle cx="12" cy="12" r="10" />
        <polyline points="12 6 12 12 14 14" />
      </svg>
    ),
  },
  INTERVIEWING: {
    label: 'Active Interviews',
    color: '#f59e0b',
    icon: (
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <rect x="3" y="4" width="18" height="18" rx="2" />
        <line x1="16" y1="2" x2="16" y2="6" />
        <line x1="8" y1="2" x2="8" y2="6" />
      </svg>
    ),
  },
  OFFER: {
    label: 'Offers Received',
    color: '#10b981',
    icon: (
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <circle cx="12" cy="8" r="7" />
        <polyline points="8.21 13.89 7 23 12 20 17 23 15.79 13.88" />
      </svg>
    ),
  },
};

export const FunnelPipelineChart = ({ funnel }) => {
  const stages = funnel?.stages || [];
  const maxVolume = Math.max(...stages.map((s) => s.count || 0), 1);

  return (
    <div className="funnel-chart-card">
      <div className="funnel-chart-header">
        <div>
          <h2 className="funnel-chart-title">Candidate Pipeline Conversion Funnel</h2>
          <p className="funnel-chart-subtitle">
            Progressive volume and transition efficiency across lifecycle stages.
          </p>
        </div>
        <div className="funnel-overall-badge">
          <span>Overall: <strong>{funnel?.overallConversionRate ?? 0}%</strong></span>
        </div>
      </div>

      <div className="funnel-stages-container">
        {stages.map((stage, idx) => {
          const config = STAGE_CONFIG[stage.stageName] || {
            label: stage.stageName,
            color: '#6366f1',
            icon: null,
          };

          const widthPct = Math.max(8, Math.round(((stage.count || 0) / maxVolume) * 100));
          const prevStage = idx > 0 ? stages[idx - 1] : null;
          const rate = stage.conversionFromPrevious !== undefined ? stage.conversionFromPrevious : (stage.conversionRate ?? 0);
          const dropOffPct = prevStage && prevStage.count > 0 && rate !== undefined
            ? Math.max(0, Math.round((100 - rate) * 10) / 10)
            : 0;

          return (
            <div key={stage.stageName} className="funnel-stage-item">
              <div className="funnel-stage-header">
                <div className="funnel-stage-name-box">
                  <div className="funnel-stage-icon" style={{ color: config.color, backgroundColor: `${config.color}15` }}>
                    {config.icon}
                  </div>
                  <div className="funnel-stage-name-labels">
                    <span className="funnel-stage-label">{config.label}</span>
                    <span className="funnel-stage-code">{stage.stageName}</span>
                  </div>
                </div>

                <div className="funnel-stage-counts">
                  <span className="funnel-stage-count-num">{stage.count}</span>
                  <span className="funnel-stage-count-unit">roles</span>
                </div>
              </div>

              <div className="funnel-bar-wrapper">
                <div className="funnel-bar-track">
                  <div
                    className="funnel-bar-fill"
                    style={{
                      width: `${widthPct}%`,
                      backgroundColor: config.color,
                      boxShadow: `0 0 12px ${config.color}30`,
                    }}
                  />
                </div>
              </div>

              {idx > 0 && (
                <div className="funnel-stage-transition-meta">
                  <span className="funnel-transition-rate">
                    Conversion: <strong>{rate}%</strong>
                  </span>
                  {dropOffPct > 0 && (
                    <span className="funnel-dropoff-pill">
                      -{dropOffPct}% drop-off
                    </span>
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};
