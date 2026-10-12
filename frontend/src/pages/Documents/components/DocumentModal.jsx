import React, { useState, useEffect } from 'react';
import { Button } from '../../../components/common/Button';
import './DocumentModal.css';

const DOCUMENT_TYPES = [
  { value: 'RESUME', label: 'Resume' },
  { value: 'COVER_LETTER', label: 'Cover Letter' },
  { value: 'PORTFOLIO', label: 'Portfolio Link' },
  { value: 'TRANSCRIPT', label: 'Transcript' },
];

export const DocumentModal = ({
  isOpen,
  onClose,
  onSave,
  initialData = null,
}) => {
  const isEdit = Boolean(initialData);

  const [title, setTitle] = useState('');
  const [documentType, setDocumentType] = useState('RESUME');
  const [storagePath, setStoragePath] = useState('');
  const [versionTag, setVersionTag] = useState('');

  const [error, setError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (initialData) {
      setTitle(initialData.title || '');
      setDocumentType(initialData.documentType || 'RESUME');
      setStoragePath(initialData.storagePath || '');
      setVersionTag(initialData.versionTag || '');
    } else {
      setTitle('');
      setDocumentType('RESUME');
      setStoragePath('');
      setVersionTag('');
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
      setError('Document title is required.');
      return;
    }
    if (!storagePath.trim()) {
      setError('Storage URL is required.');
      return;
    }
    if (!/^https?:\/\/.+/i.test(storagePath.trim())) {
      setError('Storage path must be a valid absolute HTTP or HTTPS URL (e.g. https://drive.google.com/...).');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      const payload = {
        title: title.trim(),
        documentType,
        storagePath: storagePath.trim(),
        versionTag: versionTag.trim() || null,
      };

      await onSave(payload, isEdit, initialData?.id);
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to save document');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" style={{ maxWidth: '480px' }} onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2 className="modal-title">
            {isEdit ? 'Edit Document Link' : 'Register New Document'}
          </h2>
          <button className="modal-close-btn" onClick={onClose} aria-label="Close modal">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="modal-body">
          <div className="document-modal-form">
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

            <div className="document-form-group">
              <label htmlFor="docTitle" className="document-form-label">Document Title *</label>
              <input
                id="docTitle"
                type="text"
                className="document-form-input"
                placeholder="e.g. Software Engineer Resume (General)"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                maxLength={200}
                required
              />
            </div>

            <div className="document-form-group">
              <label htmlFor="docType" className="document-form-label">Document Type *</label>
              <select
                id="docType"
                className="document-form-select"
                value={documentType}
                onChange={(e) => setDocumentType(e.target.value)}
                required
              >
                {DOCUMENT_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="document-form-group">
              <label htmlFor="docUrl" className="document-form-label">External Storage URL (Google Drive, OneDrive, Portfolio) *</label>
              <input
                id="docUrl"
                type="url"
                className="document-form-input"
                placeholder="https://drive.google.com/file/d/..."
                value={storagePath}
                onChange={(e) => setStoragePath(e.target.value)}
                maxLength={1000}
                required
              />
            </div>

            <div className="document-form-group">
              <label htmlFor="docTag" className="document-form-label">Version Tag (Optional)</label>
              <input
                id="docTag"
                type="text"
                className="document-form-input"
                placeholder="e.g. v2.1, 2026_Fall, or Backend_Focus"
                value={versionTag}
                onChange={(e) => setVersionTag(e.target.value)}
                maxLength={50}
              />
            </div>
          </div>

          <div className="modal-footer" style={{ padding: 'var(--space-4) 0 0 0', display: 'flex', justifyContent: 'flex-end', gap: 'var(--space-3)' }}>
            <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" isLoading={isSubmitting}>
              {isEdit ? 'Update Document' : 'Register Document'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
