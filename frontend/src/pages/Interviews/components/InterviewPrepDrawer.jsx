import React, { useState, useEffect } from 'react';
import { Button } from '../../../components/common/Button';
import './InterviewPrepDrawer.css';

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
    weekday: 'short',
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
};

export const InterviewPrepDrawer = ({
  interview,
  application,
  isOpen,
  onClose,
  onRecordOutcome,
  onEdit,
}) => {
  const [selectedOutcome, setSelectedOutcome] = useState('PENDING');
  const [reflectionText, setReflectionText] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    if (interview) {
      setSelectedOutcome(interview.outcome || 'PENDING');
      setReflectionText(interview.reflectionNotes || '');
    }
  }, [interview]);

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen || !interview) return null;

  const companyName = application?.companyName || 'Company';
  const roleTitle = application?.opportunityTitle || 'Internship Role';
  const roundTypeLabel = ROUND_TYPE_LABELS[interview.roundType] || interview.roundType;

  const handleSaveOutcome = async () => {
    const VALID_OUTCOMES = ['PENDING', 'PASSED', 'FAILED', 'RESCHEDULED'];
    if (!VALID_OUTCOMES.includes(selectedOutcome)) {
      return;
    }
    setIsSaving(true);
    try {
      await onRecordOutcome(interview.id, selectedOutcome, reflectionText.trim() || null);
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <>
      <div className="drawer-backdrop" onClick={onClose} />
      <aside className="interview-prep-drawer" role="dialog" aria-modal="true" aria-label="Interview Prep Details">
        <header className="drawer-header">
          <div className="drawer-header-left">
            <div className="drawer-company-row">
              <span className="drawer-company-name">{companyName} — {roleTitle}</span>
              <span className="round-number-pill">Round {interview.roundNumber}</span>
            </div>
            <h2 className="drawer-title">{interview.roundTitle}</h2>
          </div>

          <button className="drawer-close-btn" onClick={onClose} aria-label="Close drawer">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </header>

        <div className="drawer-content">
          {/* Outcome & Reflections Recorder */}
          <div className="outcome-recorder-box">
            <div className="recorder-title">Interview Outcome & Reflections</div>
            <div className="outcome-button-group" role="group" aria-label="Interview outcome choices">
              <button
                type="button"
                className={`outcome-select-btn ${selectedOutcome === 'PASSED' ? 'active-passed' : ''}`}
                aria-pressed={selectedOutcome === 'PASSED'}
                onClick={() => setSelectedOutcome('PASSED')}
              >
                ✓ Passed
              </button>
              <button
                type="button"
                className={`outcome-select-btn ${selectedOutcome === 'FAILED' ? 'active-failed' : ''}`}
                aria-pressed={selectedOutcome === 'FAILED'}
                onClick={() => setSelectedOutcome('FAILED')}
              >
                ✕ Failed
              </button>
              <button
                type="button"
                className={`outcome-select-btn ${selectedOutcome === 'RESCHEDULED' ? 'active-rescheduled' : ''}`}
                aria-pressed={selectedOutcome === 'RESCHEDULED'}
                onClick={() => setSelectedOutcome('RESCHEDULED')}
              >
                ↻ Rescheduled
              </button>
              <button
                type="button"
                className={`outcome-select-btn ${selectedOutcome === 'PENDING' ? 'active-pending' : ''}`}
                aria-pressed={selectedOutcome === 'PENDING'}
                onClick={() => setSelectedOutcome('PENDING')}
              >
                ⏱ Pending
              </button>
            </div>

            <textarea
              className="reflection-textarea"
              placeholder="Post-round reflections: questions asked, self-critique, follow-ups..."
              aria-label="Interview reflections and notes"
              value={reflectionText}
              onChange={(e) => setReflectionText(e.target.value)}
            />

            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <Button
                variant="primary"
                size="sm"
                onClick={handleSaveOutcome}
                isLoading={isSaving}
                disabled={isSaving}
              >
                Save Outcome
              </Button>
            </div>
          </div>

          {/* Preparation Notes Section */}
          <section className="prep-notes-section">
            <div className="section-header-row">
              <span className="section-title">Preparation Notes & Talking Points</span>
              <Button
                variant="secondary"
                size="sm"
                onClick={() => onEdit(interview)}
              >
                Edit Notes
              </Button>
            </div>

            {interview.prepNotes ? (
              <div className="prep-notes-viewer">
                {interview.prepNotes}
              </div>
            ) : (
              <div className="prep-notes-viewer prep-notes-empty">
                No preparation notes recorded yet. Click "Edit Notes" to write company research, technical topics, or questions for the interviewer.
              </div>
            )}
          </section>

          {/* Round Specifications Grid */}
          <section className="prep-notes-section">
            <span className="section-title">Round Specifications</span>
            <div className="drawer-specs-grid">
              <div className="spec-item">
                <span className="spec-label">Round Type</span>
                <span className="spec-value">{roundTypeLabel}</span>
              </div>
              <div className="spec-item">
                <span className="spec-label">Duration</span>
                <span className="spec-value">
                  {interview.durationMinutes ? `${interview.durationMinutes} minutes` : 'Not specified'}
                </span>
              </div>
              <div className="spec-item" style={{ gridColumn: '1 / -1' }}>
                <span className="spec-label">Scheduled Date & Time</span>
                <span className="spec-value">{formatLocalDateTime(interview.scheduledAt)}</span>
              </div>
            </div>
          </section>

          {/* Interviewer Contact Information */}
          {(interview.interviewerNames || interview.interviewerEmails) && (
            <section className="prep-notes-section">
              <span className="section-title">Interviewer Contact</span>
              <div className="drawer-specs-grid">
                {interview.interviewerNames && (
                  <div className="spec-item">
                    <span className="spec-label">Name</span>
                    <span className="spec-value">{interview.interviewerNames}</span>
                  </div>
                )}
                {interview.interviewerEmails && (
                  <div className="spec-item">
                    <span className="spec-label">Email</span>
                    <span className="spec-value">
                      <a
                        href={`mailto:${interview.interviewerEmails}`}
                        style={{ color: 'var(--color-accent-primary)', textDecoration: 'none' }}
                      >
                        {interview.interviewerEmails}
                      </a>
                    </span>
                  </div>
                )}
              </div>
            </section>
          )}
        </div>

        <footer className="drawer-footer">
          <Button variant="secondary" onClick={onClose}>
            Close
          </Button>
          <Button variant="primary" onClick={() => onEdit(interview)}>
            Edit Round Details
          </Button>
        </footer>
      </aside>
    </>
  );
};
