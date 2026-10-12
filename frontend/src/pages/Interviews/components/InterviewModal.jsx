import React, { useState, useEffect } from 'react';
import { Button } from '../../../components/common/Button';
import './InterviewModal.css';

const ROUND_TYPES = [
  { value: 'RECRUITER_SCREEN', label: 'Recruiter Screen' },
  { value: 'TECHNICAL_OA', label: 'Technical OA' },
  { value: 'LIVE_CODING', label: 'Live Coding' },
  { value: 'SYSTEM_DESIGN', label: 'System Design' },
  { value: 'BEHAVIORAL', label: 'Behavioral' },
  { value: 'EXECUTIVE_FINAL', label: 'Executive Final' },
];

const DURATION_OPTIONS = [15, 30, 45, 60, 75, 90, 120];

const toLocalDatetimeString = (isoString) => {
  if (!isoString) return '';
  const d = new Date(isoString);
  const pad = (n) => String(n).padStart(2, '0');
  const yyyy = d.getFullYear();
  const mm = pad(d.getMonth() + 1);
  const dd = pad(d.getDate());
  const hh = pad(d.getHours());
  const min = pad(d.getMinutes());
  return `${yyyy}-${mm}-${dd}T${hh}:${min}`;
};

export const InterviewModal = ({
  isOpen,
  onClose,
  onSave,
  initialData,
  applications = [],
  existingInterviews = [],
}) => {
  const isEdit = Boolean(initialData);

  const [selectedAppId, setSelectedAppId] = useState('');
  const [roundNumber, setRoundNumber] = useState(1);
  const [roundTitle, setRoundTitle] = useState('');
  const [roundType, setRoundType] = useState('RECRUITER_SCREEN');
  const [scheduledAtLocal, setScheduledAtLocal] = useState('');
  const [durationMinutes, setDurationMinutes] = useState(45);
  const [interviewerNames, setInterviewerNames] = useState('');
  const [interviewerEmails, setInterviewerEmails] = useState('');
  const [prepNotes, setPrepNotes] = useState('');
  const [reflectionNotes, setReflectionNotes] = useState('');
  const [outcome, setOutcome] = useState('PENDING');

  const [error, setError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Compute suggested round number: max(existing roundNumber for appId) + 1
  const computeSuggestedRound = (appId) => {
    if (!appId) return 1;
    const roundsForApp = existingInterviews.filter(
      (item) => String(item.applicationId) === String(appId)
    );
    if (roundsForApp.length === 0) return 1;
    const maxRound = Math.max(...roundsForApp.map((item) => item.roundNumber || 0));
    return maxRound + 1;
  };

  useEffect(() => {
    if (initialData) {
      setSelectedAppId(String(initialData.applicationId || ''));
      setRoundNumber(initialData.roundNumber || 1);
      setRoundTitle(initialData.roundTitle || '');
      setRoundType(initialData.roundType || 'RECRUITER_SCREEN');
      setScheduledAtLocal(toLocalDatetimeString(initialData.scheduledAt));
      setDurationMinutes(initialData.durationMinutes || 45);
      setInterviewerNames(initialData.interviewerNames || '');
      setInterviewerEmails(initialData.interviewerEmails || '');
      setPrepNotes(initialData.prepNotes || '');
      setReflectionNotes(initialData.reflectionNotes || '');
      setOutcome(initialData.outcome || 'PENDING');
    } else {
      const defaultAppId = applications[0]?.id ? String(applications[0].id) : '';
      setSelectedAppId(defaultAppId);
      const suggested = computeSuggestedRound(defaultAppId);
      setRoundNumber(suggested);
      setRoundTitle(suggested === 1 ? 'Recruiter Phone Screen' : `Round ${suggested} Interview`);
      setRoundType(suggested === 1 ? 'RECRUITER_SCREEN' : 'LIVE_CODING');

      // Default scheduledAt to tomorrow at 10:00 AM local time
      const tomorrow = new Date();
      tomorrow.setDate(tomorrow.getDate() + 1);
      tomorrow.setHours(10, 0, 0, 0);
      setScheduledAtLocal(toLocalDatetimeString(tomorrow.toISOString()));

      setDurationMinutes(45);
      setInterviewerNames('');
      setInterviewerEmails('');
      setPrepNotes('');
      setReflectionNotes('');
      setOutcome('PENDING');
    }
    setError(null);
  }, [initialData, isOpen, applications]);

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  // When selected application changes in create mode, re-compute suggested round
  const handleAppChange = (newAppId) => {
    setSelectedAppId(newAppId);
    if (!isEdit) {
      const suggested = computeSuggestedRound(newAppId);
      setRoundNumber(suggested);
      setRoundTitle(suggested === 1 ? 'Recruiter Phone Screen' : `Round ${suggested} Interview`);
    }
  };

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!selectedAppId) {
      setError('Please select an application for this interview.');
      return;
    }
    if (!roundTitle.trim()) {
      setError('Round title is required.');
      return;
    }
    if (!scheduledAtLocal) {
      setError('Scheduled date & time is required.');
      return;
    }
    if (!roundNumber || roundNumber < 1) {
      setError('Round number must be a positive integer.');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      // Interpret local datetime string in browser timezone and convert to UTC Instant
      const scheduledAtInstant = new Date(scheduledAtLocal).toISOString();

      const payload = {
        roundNumber: parseInt(roundNumber, 10),
        roundTitle: roundTitle.trim(),
        roundType,
        scheduledAt: scheduledAtInstant,
        durationMinutes: durationMinutes ? parseInt(durationMinutes, 10) : null,
        interviewerNames: interviewerNames.trim() || null,
        interviewerEmails: interviewerEmails.trim() || null,
        prepNotes: prepNotes.trim() || null,
      };

      if (isEdit) {
        payload.reflectionNotes = reflectionNotes.trim() || null;
        payload.outcome = outcome;
      }

      await onSave(Number(selectedAppId), payload, isEdit, initialData?.id);
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to save interview round');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2 className="modal-title">
            {isEdit ? 'Edit Interview Round' : 'Schedule Interview Round'}
          </h2>
          <button className="modal-close-btn" onClick={onClose} aria-label="Close modal">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="modal-body">
          <div className="interview-modal-form">
            {error && (
              <div
                style={{
                  padding: '8px 12px',
                  backgroundColor: 'rgba(239, 68, 68, 0.1)',
                  border: '1px solid rgba(239, 68, 68, 0.2)',
                  borderRadius: 'var(--radius-md)',
                  color: '#ef4444',
                  fontSize: 'var(--font-size-xs)',
                }}
              >
                {error}
              </div>
            )}

            <div className="interview-form-group full-width">
              <label htmlFor="interview-linked-app" className="interview-form-label">Linked Application *</label>
              <select
                id="interview-linked-app"
                className="interview-form-select"
                value={selectedAppId}
                onChange={(e) => handleAppChange(e.target.value)}
                disabled={isEdit}
                required
              >
                <option value="" disabled>Select an active application</option>
                {applications.map((app) => (
                  <option key={app.id} value={app.id}>
                    {app.companyName || 'Company'} — {app.opportunityTitle || 'Role'}
                  </option>
                ))}
              </select>
            </div>

            <div className="interview-form-group">
              <label htmlFor="interview-round-number" className="interview-form-label">Round Number *</label>
              <input
                id="interview-round-number"
                type="number"
                min="1"
                className="interview-form-input"
                value={roundNumber}
                onChange={(e) => setRoundNumber(e.target.value)}
                required
              />
            </div>

            <div className="interview-form-group">
              <label htmlFor="interview-round-type" className="interview-form-label">Round Type *</label>
              <select
                id="interview-round-type"
                className="interview-form-select"
                value={roundType}
                onChange={(e) => setRoundType(e.target.value)}
                required
              >
                {ROUND_TYPES.map((rt) => (
                  <option key={rt.value} value={rt.value}>
                    {rt.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="interview-form-group full-width">
              <label htmlFor="interview-round-title" className="interview-form-label">Round Title *</label>
              <input
                id="interview-round-title"
                type="text"
                className="interview-form-input"
                placeholder="e.g. Recruiter Phone Screen, System Design Deep Dive"
                value={roundTitle}
                onChange={(e) => setRoundTitle(e.target.value)}
                required
              />
            </div>

            <div className="interview-form-group">
              <label htmlFor="interview-scheduled-at" className="interview-form-label">Scheduled Date & Time (Local) *</label>
              <input
                id="interview-scheduled-at"
                type="datetime-local"
                className="interview-form-input"
                value={scheduledAtLocal}
                onChange={(e) => setScheduledAtLocal(e.target.value)}
                required
              />
            </div>

            <div className="interview-form-group">
              <label className="interview-form-label">Duration (Minutes)</label>
              <select
                className="interview-form-select"
                value={durationMinutes || 45}
                onChange={(e) => setDurationMinutes(Number(e.target.value))}
              >
                {DURATION_OPTIONS.map((min) => (
                  <option key={min} value={min}>
                    {min} minutes
                  </option>
                ))}
              </select>
            </div>

            <div className="interview-form-group">
              <label className="interview-form-label">Interviewer Name(s)</label>
              <input
                type="text"
                className="interview-form-input"
                placeholder="e.g. Alex Rivera, Senior Staff Engineer"
                value={interviewerNames}
                onChange={(e) => setInterviewerNames(e.target.value)}
              />
            </div>

            <div className="interview-form-group">
              <label className="interview-form-label">Interviewer Email(s)</label>
              <input
                type="email"
                className="interview-form-input"
                placeholder="e.g. interviewer@company.com"
                value={interviewerEmails}
                onChange={(e) => setInterviewerEmails(e.target.value)}
              />
            </div>

            <div className="interview-form-group full-width">
              <label className="interview-form-label">Preparation Notes & Talking Points</label>
              <textarea
                className="interview-form-textarea"
                placeholder="Technical topics to review, behavioral STAR stories, questions to ask..."
                value={prepNotes}
                onChange={(e) => setPrepNotes(e.target.value)}
              />
            </div>

            {isEdit && (
              <>
                <div className="interview-form-group">
                  <label className="interview-form-label">Interview Outcome</label>
                  <select
                    className="interview-form-select"
                    value={outcome}
                    onChange={(e) => setOutcome(e.target.value)}
                  >
                    <option value="PENDING">Pending</option>
                    <option value="PASSED">Passed</option>
                    <option value="FAILED">Failed</option>
                    <option value="RESCHEDULED">Rescheduled</option>
                  </select>
                </div>

                <div className="interview-form-group full-width">
                  <label className="interview-form-label">Post-Round Reflections</label>
                  <textarea
                    className="interview-form-textarea"
                    placeholder="How did it go? Questions asked, self-critique, follow-ups..."
                    value={reflectionNotes}
                    onChange={(e) => setReflectionNotes(e.target.value)}
                  />
                </div>
              </>
            )}
          </div>

          <div className="modal-footer" style={{ padding: 'var(--space-4) 0 0 0', display: 'flex', justifyContent: 'flex-end', gap: 'var(--space-3)' }}>
            <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" isLoading={isSubmitting}>
              {isEdit ? 'Update Round' : 'Schedule Round'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
