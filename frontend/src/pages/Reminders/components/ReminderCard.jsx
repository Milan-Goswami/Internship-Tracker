import React from 'react';
import './ReminderCard.css';

const TYPE_CONFIG = {
  OA_DEADLINE: { label: 'OA Deadline', className: 'type-oa' },
  INTERVIEW_PREP: { label: 'Interview Prep', className: 'type-interview' },
  FOLLOW_UP: { label: 'Follow Up', className: 'type-followup' },
  CUSTOM: { label: 'Task', className: 'type-custom' },
};

const formatDueDisplay = (isoString, isCompleted) => {
  if (!isoString) return { text: 'No Due Date', className: '' };
  const d = new Date(isoString);
  const now = new Date();
  
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0, 0).getTime();
  const endOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59, 999).getTime();
  const time = d.getTime();

  const formattedTime = d.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });

  if (isCompleted) {
    return {
      text: d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' }),
      className: '',
    };
  }

  if (time < startOfToday) {
    const diffDays = Math.ceil((startOfToday - time) / (1000 * 60 * 60 * 24));
    return {
      text: `Overdue by ${diffDays}d`,
      className: 'overdue',
    };
  }

  if (time >= startOfToday && time <= endOfToday) {
    return {
      text: `Today at ${formattedTime}`,
      className: 'today',
    };
  }

  return {
    text: d.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' }),
    className: '',
  };
};

export const ReminderCard = ({
  reminder,
  application,
  onToggleComplete,
  onEdit,
  onDelete,
}) => {
  const typeConfig = TYPE_CONFIG[reminder.reminderType] || TYPE_CONFIG.CUSTOM;
  const dueInfo = formatDueDisplay(reminder.dueAt, reminder.completed);

  return (
    <div className={`reminder-row ${reminder.completed ? 'is-completed' : ''}`}>
      <div className="reminder-left">
        <input
          type="checkbox"
          className="reminder-checkbox"
          checked={Boolean(reminder.completed)}
          onChange={() => onToggleComplete(reminder)}
          aria-label={`Mark "${reminder.title}" as ${reminder.completed ? 'incomplete' : 'complete'}`}
        />
        <div className="reminder-title-meta">
          <span className="reminder-title">{reminder.title}</span>
          {application && (
            <span className="reminder-company-badge">
              <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
                <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
              </svg>
              {application.companyName || 'Company'} — {application.opportunityTitle || 'Role'}
            </span>
          )}
        </div>
      </div>

      <div className="reminder-right">
        <span className={`reminder-type-pill ${typeConfig.className}`}>
          {typeConfig.label}
        </span>

        <span className={`reminder-due-badge ${dueInfo.className}`}>
          <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="12" cy="12" r="10" />
            <polyline points="12 6 12 12 16 14" />
          </svg>
          {dueInfo.text}
        </span>

        <div className="reminder-actions">
          <button
            type="button"
            className="reminder-action-btn"
            onClick={() => onEdit(reminder)}
            title="Edit reminder"
            aria-label="Edit reminder"
          >
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M12 20h9" />
              <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
            </svg>
          </button>
          <button
            type="button"
            className="reminder-action-btn delete"
            onClick={() => onDelete(reminder)}
            title="Delete reminder"
            aria-label="Delete reminder"
          >
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <polyline points="3 6 5 6 21 6" />
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  );
};
