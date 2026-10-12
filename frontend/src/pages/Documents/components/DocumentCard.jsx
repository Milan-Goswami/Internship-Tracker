import React from 'react';
import './DocumentCard.css';

const TYPE_CONFIG = {
  RESUME: { label: 'Resume', className: 'type-resume' },
  COVER_LETTER: { label: 'Cover Letter', className: 'type-cover-letter' },
  PORTFOLIO: { label: 'Portfolio', className: 'type-portfolio' },
  TRANSCRIPT: { label: 'Transcript', className: 'type-transcript' },
};

export const DocumentCard = ({ document, onEdit, onDelete }) => {
  const typeConfig = TYPE_CONFIG[document.documentType] || { label: document.documentType || 'Document', className: 'type-default' };

  const formatDate = (isoString) => {
    if (!isoString) return '';
    const d = new Date(isoString);
    return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
  };

  return (
    <div className="document-card">
      <div className="document-card-top">
        <div className="document-badge-row">
          <span className={`document-type-badge ${typeConfig.className}`}>
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
              <polyline points="14 2 14 8 20 8" />
              <line x1="16" y1="13" x2="8" y2="13" />
              <line x1="16" y1="17" x2="8" y2="17" />
              <polyline points="10 9 9 9 8 9" />
            </svg>
            {typeConfig.label}
          </span>
          {document.versionTag && (
            <span className="document-version-pill">{document.versionTag}</span>
          )}
        </div>

        <div className="document-actions">
          <button
            type="button"
            className="document-action-btn"
            onClick={() => onEdit(document)}
            title="Edit Document"
            aria-label="Edit Document"
          >
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M12 20h9" />
              <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
            </svg>
          </button>
          <button
            type="button"
            className="document-action-btn delete"
            onClick={() => onDelete(document)}
            title="Delete Document"
            aria-label="Delete Document"
          >
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <polyline points="3 6 5 6 21 6" />
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
            </svg>
          </button>
        </div>
      </div>

      <h3 className="document-title" title={document.title}>
        {document.title}
      </h3>

      <div className="document-card-footer">
        <a
          href={document.storagePath}
          target="_blank"
          rel="noopener noreferrer"
          className="document-link"
          title={`Open ${document.storagePath}`}
        >
          <span>Open External Link</span>
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
            <polyline points="15 3 21 3 21 9" />
            <line x1="10" y1="14" x2="21" y2="3" />
          </svg>
        </a>

        {document.uploadedAt && (
          <span className="document-date">
            {formatDate(document.uploadedAt)}
          </span>
        )}
      </div>
    </div>
  );
};
