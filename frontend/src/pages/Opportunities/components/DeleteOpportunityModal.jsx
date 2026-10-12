import React, { useEffect } from 'react';
import { Button } from '../../../components/common/Button';
import './OpportunityModal.css';

export const DeleteOpportunityModal = ({
  isOpen,
  onClose,
  onConfirm,
  opportunity,
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

  if (!isOpen || !opportunity) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" style={{ maxWidth: '440px' }} onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2 className="modal-title" style={{ color: '#ef4444' }}>
            Delete Custom Opportunity
          </h2>
          <button className="modal-close-btn" onClick={onClose} aria-label="Close modal">
            ✕
          </button>
        </div>

        <div className="modal-body" style={{ padding: 'var(--space-4)' }}>
          <p style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)', lineHeight: 1.5 }}>
            Are you sure you want to permanently delete{' '}
            <strong style={{ color: 'var(--color-text-primary)' }}>{opportunity.title}</strong>
            {opportunity.companyName ? ` at ${opportunity.companyName}` : ''}? This action cannot be undone.
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
            Delete Opportunity
          </Button>
        </div>
      </div>
    </div>
  );
};
