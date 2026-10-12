import React from 'react';
import './UpcomingInterviewsWidget.css';
import './PipelineFunnelWidget.css';

const STAGE_COLORS = {
  SAVED: '#94a3b8',
  APPLIED: '#3b82f6',
  SCREENING: '#8b5cf6',
  INTERVIEWING: '#f59e0b',
  OFFER: '#10b981',
};

export const PipelineFunnelWidget = ({ funnel }) => {
  const stages = funnel?.stages || [];
  const maxCount = Math.max(...stages.map((s) => s.count || 0), 1);

  return (
    <div className="dashboard-widget">
      <div className="widget-header">
        <span className="widget-title">Application Pipeline Funnel</span>
        <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-tertiary)' }}>
          Overall Conv: {funnel?.overallConversionRate || 0}%
        </span>
      </div>

      {stages.length === 0 ? (
        <div className="widget-empty">
          No pipeline conversions logged yet.
        </div>
      ) : (
        <div className="funnel-bars">
          {stages.map((stage) => {
            const widthPct = Math.min(100, Math.round(((stage.count || 0) / maxCount) * 100));
            const color = STAGE_COLORS[stage.stageName] || '#6366f1';

            return (
              <div key={stage.stageName} className="funnel-stage-row">
                <div className="funnel-stage-meta">
                  <span style={{ fontWeight: 'var(--font-weight-medium)', color: 'var(--color-text-primary)' }}>
                    {stage.stageName}
                  </span>
                  <span>
                    <strong>{stage.count}</strong>
                    {stage.conversionFromPrevious > 0 && stage.stageName !== 'SAVED' && (
                      <span style={{ color: 'var(--color-text-tertiary)', marginLeft: '6px' }}>
                        ({stage.conversionFromPrevious}%)
                      </span>
                    )}
                  </span>
                </div>
                <div className="funnel-track">
                  <div
                    className="funnel-fill"
                    style={{ width: `${widthPct}%`, backgroundColor: color }}
                  />
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
