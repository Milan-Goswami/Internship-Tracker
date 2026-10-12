import React, { useState, useEffect } from 'react';
import { Button } from '../../../components/common/Button';
import './ReminderModal.css';

const REMINDER_TYPES = [
  { value: 'OA_DEADLINE', label: 'OA Deadline' },
  { value: 'INTERVIEW_PREP', label: 'Interview Prep' },
  { value: 'FOLLOW_UP', label: 'Follow Up' },
  { value: 'CUSTOM', label: 'Custom Task' },
];

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

export const ReminderModal = ({
  isOpen,
  onClose,
  onSave,
  initialData,
  applications = [],
}) => {
  const isEdit = Boolean(initialData);

  const [title, setTitle] = useState('');
  const [selectedAppId, setSelectedAppId] = useState('');
  const [reminderType, setReminderType] = useState('OA_DEADLINE');
  const [dueAtLocal, setDueAtLocal] = useState('');
  const [completed, setCompleted] = useState(false);

  const [error, setError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (initialData) {
      setTitle(initialData.title || '');
      setSelectedAppId(initialData.applicationId ? String(initialData.applicationId) : '');
      setReminderType(initialData.reminderType || 'OA_DEADLINE');
      setDueAtLocal(toLocalDatetimeString(initialData.dueAt));
      setCompleted(Boolean(initialData.completed));
    } else {
      setTitle('');
      setSelectedAppId('');
      setReminderType('OA_DEADLINE');
      
      // Default to today at 18:00
      const defaultTime = new Date();
      defaultTime.setHours(18, 0, 0, 0);
      setDueAtLocal(toLocalDatetimeString(defaultTime.toISOString()));
      setCompleted(false);
    }
    setError(null);
  }, [initialData, isOpen]);

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!title.trim()) {
      setError('Reminder title is required.');
      return;
    }
    if (!dueAtLocal) {
      setError('Due date & time is required.');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      // Interpret local datetime string in browser timezone and convert to UTC Instant
      const dueAtInstant = new Date(dueAtLocal).toISOString();

      const payload = {
        title: title.trim(),
        dueAt: dueAtInstant,
        reminderType,
        applicationId: selectedAppId ? Number(selectedAppId) : null,
      };

      if (isEdit) {
        payload.completed = completed;
      }

      await onSave(payload, isEdit, initialData?.id);
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to save reminder');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" style={{ maxWidth: '480px' }} onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2 className="modal-title">
            {isEdit ? 'Edit Reminder' : 'Create Reminder'}
          </h2>
          <button className="modal-close-btn" onClick={onClose} aria-label="Close modal">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="modal-body">
          <div className="reminder-modal-form">
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

            <div className="reminder-form-group">
              <label className="reminder-form-label">Task Title *</label>
              <input
                type="text"
                className="reminder-form-input"
                placeholder="e.g. Complete HackerRank OA for Datadog"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                required
              />
            </div>

            <div className="reminder-form-group">
              <label className="reminder-form-label">Linked Application (Optional)</label>
              <select
                className="reminder-form-select"
                value={selectedAppId}
                onChange={(e) => setSelectedAppId(e.target.value)}
              >
                <option value="">-- No linked application --</option>
                {applications.map((app) => (
                  <option key={app.id} value={app.id}>
                    {app.companyName || 'Company'} — {app.opportunityTitle || 'Role'}
                  </option>
                ))}
              </select>
            </div>

            <div className="reminder-form-group">
              <label className="reminder-form-label">Reminder Type *</label>
              <select
                className="reminder-form-select"
                value={reminderType}
                onChange={(e) => setReminderType(e.target.value)}
                required
              >
                {REMINDER_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="reminder-form-group">
              <label className="reminder-form-label">Due Date & Time (Local) *</label>
              <input
                type="datetime-local"
                className="reminder-form-input"
                value={dueAtLocal}
                onChange={(e) => setDueAtLocal(e.target.value)}
                required
              />
            </div>
          </div>

          <div className="modal-footer" style={{ padding: 'var(--space-4) 0 0 0', display: 'flex', justifyContent: 'flex-end', gap: 'var(--space-3)' }}>
            <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" isLoading={isSubmitting}>
              {isEdit ? 'Update Reminder' : 'Create Reminder'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
