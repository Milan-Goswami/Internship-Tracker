import React, { useEffect } from 'react';
import { Button } from '../../../components/common/Button';
import './ApplicationModal.css';

export const DeleteConfirmModal = ({
  isOpen,
  onClose,
  onConfirm,
  application,
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

  if (!isOpen || !application) {
    return null;
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" style={{ maxWidth: '440px' }} onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2 className="modal-title" style={{ color: 'var(--color-status-rejected)' }}>
            Delete Application
          </h2>
          <button className="modal-close-btn" onClick={onClose} aria-label="Close modal">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <div className="modal-body">
          <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)', lineHeight: 1.5 }}>
            Are you sure you want to remove your application for{' '}
            <strong style={{ color: 'var(--color-text-primary)' }}>
              {application.companyName || 'Company'} — {application.opportunityTitle || 'Role'}
            </strong>
            ? This action cannot be undone.
          </p>
        </div>

        <div className="modal-footer">
          <Button variant="secondary" size="md" onClick={onClose} disabled={isDeleting}>
            Cancel
          </Button>
          <Button
            variant="danger"
            size="md"
            onClick={onConfirm}
            isLoading={isDeleting}
          >
            Delete Application
          </Button>
        </div>
      </div>
    </div>
  );
};