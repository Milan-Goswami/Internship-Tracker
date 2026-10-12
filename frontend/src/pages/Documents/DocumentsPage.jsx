import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { documentService } from '../../services/documentService';
import { DocumentCard } from './components/DocumentCard';
import { DocumentModal } from './components/DocumentModal';
import { DeleteDocumentModal } from './components/DeleteDocumentModal';
import { EmptyState } from '../../components/states/EmptyState';
import { ErrorState } from '../../components/states/ErrorState';
import { Skeleton } from '../../components/common/Skeleton';
import { Button } from '../../components/common/Button';
import { useToast } from '../../context/ToastContext';
import './DocumentsPage.css';

const TABS = [
  { id: 'ALL', label: 'All Documents' },
  { id: 'RESUME', label: 'Resumes' },
  { id: 'COVER_LETTER', label: 'Cover Letters' },
  { id: 'PORTFOLIO', label: 'Portfolio Links' },
  { id: 'TRANSCRIPT', label: 'Transcripts' },
];

export const DocumentsLibrary = () => {
  const [documents, setDocuments] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const [activeTab, setActiveTab] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalInitialData, setModalInitialData] = useState(null);

  const [deleteTarget, setDeleteTarget] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const { showToast } = useToast();

  const fetchDocuments = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await documentService.getDocuments();
      setDocuments(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to load documents');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDocuments();
  }, [fetchDocuments]);

  // Client-side filtering across active tab and search query
  const filteredDocuments = useMemo(() => {
    return documents.filter((doc) => {
      if (activeTab !== 'ALL' && doc.documentType !== activeTab) {
        return false;
      }
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matchesTitle = (doc.title || '').toLowerCase().includes(q);
        const matchesVersion = (doc.versionTag || '').toLowerCase().includes(q);
        if (!matchesTitle && !matchesVersion) return false;
      }
      return true;
    });
  }, [documents, activeTab, searchQuery]);

  // Handle Create or Update
  const handleSaveDocument = async (payload, isEdit, documentId) => {
    if (isEdit) {
      const updated = await documentService.updateDocument(documentId, payload);
      setDocuments((prev) =>
        prev.map((d) => (d.id === documentId ? { ...d, ...updated } : d))
      );
      showToast('Document updated successfully', 'success');
    } else {
      const created = await documentService.createDocument(payload);
      setDocuments((prev) => [created, ...prev]);
      showToast('Document registered successfully', 'success');
    }
  };

  // Handle Delete with explicit rollback on error
  const handleDeleteDocument = async (doc) => {
    const prevList = documents;
    setIsDeleting(true);

    // Optimistic local removal
    setDocuments((prev) => prev.filter((d) => d.id !== doc.id));

    try {
      await documentService.deleteDocument(doc.id);
      showToast('Document deleted successfully', 'success');
      setDeleteTarget(null);
    } catch (err) {
      // Rollback on failure
      setDocuments(prevList);
      showToast(err.response?.data?.message || 'Failed to delete document', 'error');
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div className="documents-page">
      <header className="documents-page-header">
        <div>
          <h1 className="documents-page-title">Documents Library</h1>
          <p className="documents-page-subtitle">
            Resume versions, tailored cover letters, and portfolio links associated with applications.
          </p>
        </div>
        <Button
          variant="primary"
          onClick={() => {
            setModalInitialData(null);
            setIsModalOpen(true);
          }}
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          Register Document
        </Button>
      </header>

      {/* Toolbar: Filter Tabs & Search Input */}
      <div className="documents-toolbar">
        <div className="documents-tabs" role="tablist">
          {TABS.map((tab) => (
            <button
              key={tab.id}
              role="tab"
              aria-selected={activeTab === tab.id}
              className={`documents-tab-btn ${activeTab === tab.id ? 'active' : ''}`}
              onClick={() => setActiveTab(tab.id)}
            >
              {tab.label}
              <span className="documents-tab-count">
                {tab.id === 'ALL'
                  ? documents.length
                  : documents.filter((d) => d.documentType === tab.id).length}
              </span>
            </button>
          ))}
        </div>

        <div className="documents-search-box">
          <svg className="documents-search-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            type="text"
            className="documents-search-input"
            placeholder="Search documents by title or version..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
          {searchQuery && (
            <button
              type="button"
              className="documents-search-clear"
              onClick={() => setSearchQuery('')}
              aria-label="Clear search"
            >
              &times;
            </button>
          )}
        </div>
      </div>

      {/* Content Rendering: 7 UI States */}
      {isLoading ? (
        <div className="documents-grid">
          <Skeleton width="100%" height="150px" style={{ borderRadius: '12px' }} />
          <Skeleton width="100%" height="150px" style={{ borderRadius: '12px' }} />
          <Skeleton width="100%" height="150px" style={{ borderRadius: '12px' }} />
        </div>
      ) : error ? (
        <ErrorState
          title="Failed to Load Documents"
          description={error}
          onRetry={fetchDocuments}
        />
      ) : documents.length === 0 ? (
        <EmptyState
          title="No Documents Registered Yet"
          description="Track tailored resumes, cover letters, and portfolio links. Associate them with specific job applications to prevent context collapse."
          actionLabel="+ Register Your First Document"
          onAction={() => {
            setModalInitialData(null);
            setIsModalOpen(true);
          }}
        />
      ) : filteredDocuments.length === 0 ? (
        <EmptyState
          title="No Matching Documents"
          description="No documents match your active filter or search query. Try broadening your criteria."
          actionLabel="Reset Filters"
          onAction={() => {
            setActiveTab('ALL');
            setSearchQuery('');
          }}
        />
      ) : (
        <div className="documents-grid">
          {filteredDocuments.map((doc) => (
            <DocumentCard
              key={doc.id}
              document={doc}
              onEdit={(d) => {
                setModalInitialData(d);
                setIsModalOpen(true);
              }}
              onDelete={(d) => setDeleteTarget(d)}
            />
          ))}
        </div>
      )}

      {/* Register / Edit Modal */}
      <DocumentModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSave={handleSaveDocument}
        initialData={modalInitialData}
      />

      {/* Delete Confirmation Modal */}
      <DeleteDocumentModal
        isOpen={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDeleteDocument}
        document={deleteTarget}
        isDeleting={isDeleting}
      />
    </div>
  );
};

export const DocumentsPage = DocumentsLibrary;
export default DocumentsLibrary;
