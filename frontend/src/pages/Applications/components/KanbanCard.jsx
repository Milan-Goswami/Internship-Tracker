import React from 'react';
import './Kanban.css';

const STAGES = [
  'SAVED',
  'APPLIED',
  'SCREENING',
  'INTERVIEWING',
  'OFFER',
  'ACCEPTED',
  'REJECTED',
  'WITHDRAWN',
];

export const KanbanCard = ({
  application,
  onStatusChange,
  onEdit,
  onDelete,
}) => {
  const currentIndex = STAGES.indexOf(application.status);
  const canMoveBack = currentIndex > 0;
  const canMoveForward = currentIndex >= 0 && currentIndex < 4; // Forward up to OFFER

  const handlePrevStage = (e) => {
    e.stopPropagation();
    if (canMoveBack) {
      onStatusChange(application.id, STAGES[currentIndex - 1]);
    }
  };

  const handleNextStage = (e) => {
    e.stopPropagation();
    if (canMoveForward) {
      onStatusChange(application.id, STAGES[currentIndex + 1]);
    }
  };

  const handleStageSelect = (e) => {
    e.stopPropagation();
    onStatusChange(application.id, e.target.value);
  };

  const formattedDate = application.appliedAt
    ? new Date(application.appliedAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
    : application.savedAt
    ? new Date(application.savedAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
    : 'Recently';

  const companyInitial = application.companyName ? application.companyName.charAt(0).toUpperCase() : 'O';

  return (
    <div className="kanban-card">
      <div className="kanban-card-top">
        <div className="card-company-info">
          <div className="card-company-avatar">{companyInitial}</div>
          <span className="card-company-name" title={application.companyName}>
            {application.companyName || 'Unknown Company'}
          </span>
        </div>

        <div style={{ display: 'flex', gap: '4px' }}>
          <button
            className="card-actions-menu-btn"
            onClick={() => onEdit(application)}
            title="Edit Application"
            aria-label={`Edit application for ${application.companyName || 'application'}`}
          >
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
            </svg>
          </button>
          <button
            className="card-actions-menu-btn"
            onClick={() => onDelete(application)}
            title="Delete Application"
            aria-label={`Delete application for ${application.companyName || 'application'}`}
            style={{ color: 'var(--color-status-rejected)' }}
          >
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <polyline points="3 6 5 6 21 6" />
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
            </svg>
          </button>
        </div>
      </div>

      <div className="card-role-title" title={application.opportunityTitle}>
        {application.opportunityTitle || 'Software Engineering Intern'}
      </div>

      {application.userRating && (
        <div className="card-rating-row" title={`Rating: ${application.userRating} / 5`}>
          {[1, 2, 3, 4, 5].map((star) => (
            <span
              key={star}
              className={`star-icon ${star <= application.userRating ? 'filled' : 'empty'}`}
            >
              ★
            </span>
          ))}
        </div>
      )}

      {application.personalNotes && (
        <div className="card-notes-preview" title={application.personalNotes}>
          {application.personalNotes}
        </div>
      )}

      <div className="kanban-card-footer">
        <span className="card-date-badge">
          {application.appliedAt ? `Applied: ${formattedDate}` : `Saved: ${formattedDate}`}
        </span>

        <div className="card-stage-nav">
          <button
            className="stage-move-btn"
            onClick={handlePrevStage}
            disabled={!canMoveBack}
            title="Move to Previous Stage"
            aria-label={`Move ${application.companyName || 'application'} to previous stage`}
          >
            ‹
          </button>

          <select
            className="stage-jump-select"
            value={application.status}
            onChange={handleStageSelect}
            title="Jump to Stage"
            aria-label={`Stage for ${application.companyName || 'application'}`}
          >
            {STAGES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>

          <button
            className="stage-move-btn"
            onClick={handleNextStage}
            disabled={!canMoveForward}
            title="Move to Next Stage"
            aria-label={`Move ${application.companyName || 'application'} to next stage`}
          >
            ›
          </button>
        </div>
      </div>
    </div>
  );
};