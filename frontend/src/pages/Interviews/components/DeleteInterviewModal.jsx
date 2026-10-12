import React, { useEffect } from 'react';
import { Button } from '../../../components/common/Button';
import './InterviewModal.css';

export const DeleteInterviewModal = ({
  isOpen,
  onClose,
  onConfirm,
  interview,
  isDeleting,
}) => {
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

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" style={{ maxWidth: '440px' }} onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2 className="modal-title" style={{ color: '#ef4444' }}>
            Delete Interview Round
          </h2>
          <button className="modal-close-btn" onClick={onClose} aria-label="Close modal">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <div className="modal-body" style={{ padding: 'var(--space-4)' }}>
          <p style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)', lineHeight: 1.5 }}>
            Are you sure you want to permanently delete{' '}
            <strong style={{ color: 'var(--color-text-primary)' }}>
              Round {interview.roundNumber}: {interview.roundTitle}
            </strong>
            ? This will remove all associated preparation and reflection notes.
          </p>
        </div>

        <div className="modal-footer" style={{ padding: 'var(--space-4)', display: 'flex', justifyContent: 'flex-end', gap: 'var(--space-3)' }}>
          <Button variant="secondary" onClick={onClose} disabled={isDeleting}>
            Cancel
          </Button>
          <Button
            variant="danger"
            onClick={onConfirm}
            isLoading={isDeleting}
          >
            Delete Round
          </Button>
        </div>
      </div>
    </div>
  );
};
