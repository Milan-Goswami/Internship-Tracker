import React from 'react';
import { useNavigate } from 'react-router-dom';
import './UpcomingInterviewsWidget.css';

const formatDueTime = (isoString) => {
  if (!isoString) return 'Date TBD';
  const d = new Date(isoString);
  return d.toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
};

export const ActiveRemindersWidget = ({ reminders = [], onToggleComplete }) => {
  const navigate = useNavigate();

  return (
    <div className="dashboard-widget">
      <div className="widget-header">
        <span className="widget-title">Active Deadlines & Reminders</span>
        <span className="widget-link" onClick={() => navigate('/reminders')}>
          Manage All &rarr;
        </span>
      </div>

      {reminders.length === 0 ? (
        <div className="widget-empty">
          No urgent deadlines or reminders due. Great job staying ahead!
        </div>
      ) : (
        <div className="interview-preview-list">
          {reminders.slice(0, 5).map((r) => (
            <div key={r.id} className="interview-preview-item">
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', minWidth: 0 }}>
                <input
                  type="checkbox"
                  checked={Boolean(r.completed)}
                  onChange={() => onToggleComplete(r)}
                  style={{ cursor: 'pointer', accentColor: 'var(--color-accent-primary)' }}
                  aria-label={`Mark "${r.title}" complete`}
                />
                <span
                  style={{
                    fontSize: 'var(--font-size-xs)',
                    color: r.completed ? 'var(--color-text-secondary)' : 'var(--color-text-primary)',
                    textDecoration: r.completed ? 'line-through' : 'none',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                  }}
                >
                  {r.title}
                </span>
              </div>
              <span
                style={{
                  fontSize: '11px',
                  color: 'var(--color-text-tertiary)',
                  flexShrink: 0,
                }}
              >
                {formatDueTime(r.dueAt)}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
