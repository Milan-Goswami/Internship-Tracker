import React from 'react';
import { Button } from '../../../components/common/Button';
import './InterviewCard.css';

const ROUND_TYPE_LABELS = {
  RECRUITER_SCREEN: 'Recruiter Screen',
  TECHNICAL_OA: 'Technical OA',
  LIVE_CODING: 'Live Coding',
  SYSTEM_DESIGN: 'System Design',
  BEHAVIORAL: 'Behavioral',
  EXECUTIVE_FINAL: 'Executive Final',
};

const formatLocalDateTime = (isoString) => {
  if (!isoString) return 'Date TBD';
  const d = new Date(isoString);
  return d.toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
};

const formatCountdown = (isoString, now = Date.now()) => {
  if (!isoString) return { text: 'Scheduled', isPast: false };
  const target = new Date(isoString).getTime();
  if (isNaN(target)) return { text: 'Scheduled', isPast: false };
  const diffMs = target - now;

  if (diffMs <= 0) {
    return { text: 'Completed', isPast: true };
  }

  const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
  const diffDays = Math.floor(diffHours / 24);

  if (diffDays > 1) {
    return { text: `In ${diffDays} days`, isPast: false };
  }
  if (diffDays === 1) {
    return { text: 'Tomorrow', isPast: false };
  }
  if (diffHours >= 1) {
    return { text: `Today in ${diffHours} hr`, isPast: false };
  }
  const diffMins = Math.floor(diffMs / (1000 * 60));
  if (diffMins > 0) {
    return { text: `In ${diffMins} min`, isPast: false };
  }
  return { text: 'In < 1 min', isPast: false };
};

export const InterviewCard = ({
  interview,
  application,
  now = Date.now(),
  onOpenDrawer,
  onRecordOutcome,
  onEdit,
  onDelete,
}) => {
  const companyName = application?.companyName || 'Company';
  const roleTitle = application?.opportunityTitle || 'Internship Role';
  const initial = companyName.charAt(0).toUpperCase();

  const roundTypeLabel = ROUND_TYPE_LABELS[interview.roundType] || interview.roundType;
  const countdown = formatCountdown(interview.scheduledAt, now);
  const formattedTime = formatLocalDateTime(interview.scheduledAt);

  const outcome = interview.outcome || 'PENDING';
  const outcomeClass =
    outcome === 'PASSED'
      ? 'outcome-passed'
      : outcome === 'FAILED'
      ? 'outcome-failed'
      : outcome === 'RESCHEDULED'
      ? 'outcome-rescheduled'
      : 'outcome-pending';

  return (
    <div className="interview-card">
      <div>
        <div className="interview-card-header">
          <div className="interview-company-meta">
            <div className="interview-company-avatar">
              <span>{initial}</span>
            </div>
            <div style={{ minWidth: 0 }}>
              <div className="interview-company-name">{companyName}</div>
              <div className="interview-role-title">{roleTitle}</div>
            </div>
          </div>

          <span className={`outcome-pill ${outcomeClass}`}>
            {outcome}
          </span>
        </div>

        <div className="interview-round-row">
          <span className="round-number-pill">Round {interview.roundNumber}</span>
          <span className="round-type-chip">{roundTypeLabel}</span>
        </div>

        <h3
          className="interview-title"
          onClick={() => onOpenDrawer(interview)}
          title="Open prep notes & details"
        >
          {interview.roundTitle}
        </h3>

        <div className="interview-time-banner">
          <div className="time-details">
            <span className="time-main">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ marginRight: '5px', verticalAlign: '-1px' }}>
                <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
                <line x1="16" y1="2" x2="16" y2="6" />
                <line x1="8" y1="2" x2="8" y2="6" />
                <line x1="3" y1="10" x2="21" y2="10" />
              </svg>
              {formattedTime}
            </span>
            {interview.durationMinutes && (
              <span className="time-duration">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ marginRight: '4px', verticalAlign: '-1px' }}>
                  <circle cx="12" cy="12" r="10" />
                  <polyline points="12 6 12 12 16 14" />
                </svg>
                {interview.durationMinutes} minutes
              </span>
            )}
          </div>
          <span className={`countdown-badge ${countdown.isPast ? 'countdown-past' : 'countdown-upcoming'}`}>
            {countdown.text}
          </span>
        </div>

        {interview.interviewerNames && (
          <div className="interview-contacts">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ flexShrink: 0 }}>
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
              <circle cx="12" cy="7" r="4" />
            </svg>
            <span style={{ color: 'var(--color-text-secondary)', marginRight: '4px' }}>Interviewer:</span>
            <strong style={{ color: 'var(--color-text-primary)' }}>{interview.interviewerNames}</strong>
            {interview.interviewerEmails && (
              <a
                href={`mailto:${interview.interviewerEmails}`}
                style={{ color: 'var(--color-accent-primary)', textDecoration: 'none', marginLeft: '6px', display: 'inline-flex', alignItems: 'center' }}
                title={`Email ${interview.interviewerEmails}`}
              >
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
                  <polyline points="22,6 12,13 2,6" />
                </svg>
              </a>
            )}
          </div>
        )}
      </div>

      <div className="interview-card-actions">
        <select
          className="interview-select"
          style={{ height: '30px', fontSize: '11px' }}
          value={outcome}
          onChange={(e) => onRecordOutcome(interview.id, e.target.value, interview.reflectionNotes)}
          aria-label="Update interview outcome"
        >
          <option value="PENDING">Pending</option>
          <option value="PASSED">Passed</option>
          <option value="FAILED">Failed</option>
          <option value="RESCHEDULED">Rescheduled</option>
        </select>

        <div className="card-button-group">
          <Button
            variant="secondary"
            size="sm"
            onClick={() => onOpenDrawer(interview)}
          >
            Prep Notes
          </Button>

          <button
            type="button"
            className="card-icon-btn"
            onClick={() => onEdit(interview)}
            title="Edit round details"
            aria-label="Edit round details"
          >
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M12 20h9" />
              <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
            </svg>
          </button>
          <button
            type="button"
            className="card-icon-btn delete"
            onClick={() => onDelete(interview)}
            title="Delete interview round"
            aria-label="Delete interview round"
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
