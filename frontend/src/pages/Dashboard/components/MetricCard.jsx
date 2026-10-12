import React from 'react';
import './MetricCard.css';

export const MetricCard = ({
  label,
  value,
  subtext,
  icon,
  iconBg = 'rgba(99, 102, 241, 0.15)',
  iconColor = '#818cf8',
}) => {
  return (
    <div className="metric-card">
      <div className="metric-header">
        <span className="metric-label">{label}</span>
        <div className="metric-icon-wrap" style={{ backgroundColor: iconBg, color: iconColor }}>
          {icon}
        </div>
      </div>

      <div>
        <div className="metric-value-row">
          <span className="metric-value">{value}</span>
        </div>
        {subtext && <div className="metric-subtext">{subtext}</div>}
      </div>
    </div>
  );
};
