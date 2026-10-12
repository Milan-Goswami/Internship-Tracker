import React from 'react';
import { useNavigate } from 'react-router-dom';
import './UpcomingInterviewsWidget.css';

const formatInterviewDate = (isoString) => {
  if (!isoString) return 'Date TBD';
  const d = new Date(isoString);
  return d.toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
};

export const UpcomingInterviewsWidget = ({ interviews = [] }) => {
  const navigate = useNavigate();

  return (
    <div className="dashboard-widget">
      <div className="widget-header">
        <span className="widget-title">Upcoming Interviews</span>
        <span className="widget-link" onClick={() => navigate('/interviews')}>
          View All &rarr;
        </span>
      </div>

      {interviews.length === 0 ? (
        <div className="widget-empty">
          No upcoming interview rounds scheduled. Log an interview when recruiters reach out.
        </div>
      ) : (
        <div className="interview-preview-list">
          {interviews.slice(0, 3).map((item) => (
            <div key={item.id} className="interview-preview-item">
              <div className="interview-preview-meta">
                <div className="interview-preview-title">{item.roundTitle}</div>
                <div className="interview-preview-comp">
                  {item.companyName || 'Company'} &bull; Round {item.roundNumber}
                </div>
              </div>
              <span className="interview-preview-badge">
                {formatInterviewDate(item.scheduledAt)}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
