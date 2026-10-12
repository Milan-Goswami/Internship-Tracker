import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { applicationService } from '../../services/applicationService';
import { ApplicationsFilterBar } from './components/ApplicationsFilterBar';
import { KanbanBoard } from './components/KanbanBoard';
import { ApplicationsTable } from './components/ApplicationsTable';
import { BatchActionToolbar } from './components/BatchActionToolbar';
import { ApplicationModal } from './components/ApplicationModal';
import { DeleteConfirmModal } from './components/DeleteConfirmModal';
import { EmptyState } from '../../components/states/EmptyState';
import { ErrorState } from '../../components/states/ErrorState';
import { Skeleton } from '../../components/common/Skeleton';
import { useToast } from '../../context/ToastContext';
import './ApplicationsPage.css';

const STAGE_ORDER = [
  'SAVED',
  'APPLIED',
  'SCREENING',
  'INTERVIEWING',
  'OFFER',
  'ACCEPTED',
  'REJECTED',
  'WITHDRAWN',
];

export const ApplicationsWorkspace = () => {
  const [applications, setApplications] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  // View Mode & Filters
  const [viewMode, setViewMode] = useState(() => {
    const params = new URLSearchParams(window.location.search);
    return params.get('view') === 'table' ? 'table' : 'kanban';
  });
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [ratingFilter, setRatingFilter] = useState('ALL');

  // Sorting for Table
  const [sortField, setSortField] = useState('date');
  const [sortOrder, setSortOrder] = useState('desc');

  // Batch Selection
  const [selectedIds, setSelectedIds] = useState([]);
  const [isBatchUpdating, setIsBatchUpdating] = useState(false);

  // Modals
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalInitialData, setModalInitialData] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const { showToast } = useToast();

  const fetchApplications = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await applicationService.getApplications();
      setApplications(data || []);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to load applications');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchApplications();
  }, [fetchApplications]);

  // Filtering
  const filteredApplications = useMemo(() => {
    return applications.filter((app) => {
      // 1. Search Query
      if (searchQuery.trim()) {
        const query = searchQuery.toLowerCase();
        const comp = (app.companyName || '').toLowerCase();
        const role = (app.opportunityTitle || '').toLowerCase();
        const notes = (app.personalNotes || '').toLowerCase();
        if (!comp.includes(query) && !role.includes(query) && !notes.includes(query)) {
          return false;
        }
      }

      // 2. Status Filter
      if (statusFilter !== 'ALL' && app.status !== statusFilter) {
        return false;
      }

      // 3. Rating Filter
      if (ratingFilter === '5' && app.userRating !== 5) {
        return false;
      }
      if (ratingFilter === '4+' && (!app.userRating || app.userRating < 4)) {
        return false;
      }
      if (ratingFilter === '3+' && (!app.userRating || app.userRating < 3)) {
        return false;
      }

      return true;
    });
  }, [applications, searchQuery, statusFilter, ratingFilter]);

  // Sorting
  const sortedApplications = useMemo(() => {
    const list = [...filteredApplications];
    list.sort((a, b) => {
      let comparison = 0;
      if (sortField === 'companyName') {
        comparison = (a.companyName || '').localeCompare(b.companyName || '');
      } else if (sortField === 'opportunityTitle') {
        comparison = (a.opportunityTitle || '').localeCompare(b.opportunityTitle || '');
      } else if (sortField === 'status') {
        const indexA = STAGE_ORDER.indexOf(a.status);
        const indexB = STAGE_ORDER.indexOf(b.status);
        comparison = indexA - indexB;
      } else if (sortField === 'userRating') {
        comparison = (a.userRating || 0) - (b.userRating || 0);
      } else {
        // Date (appliedAt or savedAt)
        const dateA = new Date(a.appliedAt || a.savedAt || 0).getTime();
        const dateB = new Date(b.appliedAt || b.savedAt || 0).getTime();
        comparison = dateA - dateB;
      }

      return sortOrder === 'asc' ? comparison : -comparison;
    });
    return list;
  }, [filteredApplications, sortField, sortOrder]);

  const handleSort = (field) => {
    if (sortField === field) {
      setSortOrder((prev) => (prev === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortField(field);
      setSortOrder('asc');
    }
  };

  // Status Transitions
  const handleStatusChange = async (id, newStatus) => {
    if (typeof navigator !== 'undefined' && !navigator.onLine) {
      showToast('Cannot update status while offline', 'warning');
      return;
    }
    try {
      const updated = await applicationService.updateStatus(id, newStatus);
      setApplications((prev) =>
        prev.map((app) => (app.id === id ? { ...app, ...updated } : app))
      );
      showToast(`Moved to ${newStatus}`, 'success');
    } catch (err) {
      showToast(err.response?.data?.message || 'Failed to update status', 'error');
    }
  };

  // Batch Status Transition
  const handleBatchUpdateStatus = async (newStatus) => {
    if (selectedIds.length === 0) return;
    if (typeof navigator !== 'undefined' && !navigator.onLine) {
      showToast('Cannot perform batch updates while offline', 'warning');
      return;
    }
    setIsBatchUpdating(true);
    try {
      const updatedList = await applicationService.batchUpdateStatus(selectedIds, newStatus);
      setApplications((prev) =>
        prev.map((app) => {
          const match = updatedList.find((u) => u.id === app.id);
          return match ? { ...app, ...match } : app;
        })
      );
      showToast(`Updated ${selectedIds.length} application${selectedIds.length > 1 ? 's' : ''} to ${newStatus}`, 'success');
      setSelectedIds([]);
    } catch (err) {
      showToast(err.response?.data?.message || 'Failed to batch update', 'error');
    } finally {
      setIsBatchUpdating(false);
    }
  };

  // Selection
  const handleToggleSelect = (id) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  };

  const handleToggleSelectAll = () => {
    const visibleIds = sortedApplications.map((app) => app.id);
    const isAllVisibleSelected = visibleIds.every((id) => selectedIds.includes(id));

    if (isAllVisibleSelected) {
      setSelectedIds((prev) => prev.filter((id) => !visibleIds.includes(id)));
    } else {
      setSelectedIds((prev) => Array.from(new Set([...prev, ...visibleIds])));
    }
  };

  // Modals
  const handleOpenAddModal = () => {
    setModalInitialData(null);
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (application) => {
    setModalInitialData(application);
    setIsModalOpen(true);
  };

  // Global keyboard shortcut for 'n' (New Application)
  useEffect(() => {
    const handleGlobalKeyDown = (e) => {
      // Do not trigger if modifier keys are pressed (e.g., Ctrl+N opens new browser window)
      if (e.ctrlKey || e.altKey || e.metaKey) {
        return;
      }

      if (e.key === 'n' || e.key === 'N') {
        const target = e.target;
        const isEditable =
          target.tagName === 'INPUT' ||
          target.tagName === 'TEXTAREA' ||
          target.tagName === 'SELECT' ||
          target.isContentEditable;

        // Do not intercept if user is typing in form control or focused on button/link
        if (isEditable || target.tagName === 'BUTTON' || target.tagName === 'A') {
          return;
        }

        // Do not trigger if any modal or drawer is already active
        const isModalActive = Boolean(
          isModalOpen ||
          deleteTarget ||
          document.querySelector('.modal-overlay, [role="dialog"], [aria-modal="true"]')
        );
        if (isModalActive) {
          return;
        }

        // Check if single-key shortcuts are disabled by user preference (WCAG 2.1 SC 2.1.4)
        if (
          typeof window !== 'undefined' &&
          window.localStorage?.getItem('interntrack_disable_single_key_shortcuts') === 'true'
        ) {
          return;
        }

        e.preventDefault();
        handleOpenAddModal();
      }
    };

    window.addEventListener('keydown', handleGlobalKeyDown);
    return () => window.removeEventListener('keydown', handleGlobalKeyDown);
  }, [isModalOpen, deleteTarget]);

  const handleSaveModal = async (payload) => {
    if (payload.id) {
      // Edit
      const updated = await applicationService.updateApplication(payload.id, {
        personalNotes: payload.personalNotes,
        userRating: payload.userRating,
      });

      // Also update status if changed
      let finalApp = updated;
      if (payload.status && payload.status !== modalInitialData.status) {
        const statusUpdated = await applicationService.updateStatus(payload.id, payload.status);
        finalApp = { ...finalApp, ...statusUpdated };
      }

      setApplications((prev) =>
        prev.map((app) => (app.id === payload.id ? { ...app, ...finalApp } : app))
      );
      showToast('Application updated successfully', 'success');
    } else {
      // Create
      const created = await applicationService.createApplication(payload);
      setApplications((prev) => [created, ...prev]);
      showToast('Application tracked successfully', 'success');
    }
  };

  // Delete
  const handleConfirmDelete = async () => {
    if (!deleteTarget) return;
    setIsDeleting(true);
    try {
      await applicationService.deleteApplication(deleteTarget.id);
      setApplications((prev) => prev.filter((app) => app.id !== deleteTarget.id));
      setSelectedIds((prev) => prev.filter((id) => id !== deleteTarget.id));
      showToast('Application removed', 'info');
      setDeleteTarget(null);
    } catch (err) {
      showToast(err.response?.data?.message || 'Failed to delete application', 'error');
    } finally {
      setIsDeleting(false);
    }
  };

  const handleClearFilters = () => {
    setSearchQuery('');
    setStatusFilter('ALL');
    setRatingFilter('ALL');
  };

  return (
    <div className="applications-page">
      <header className="applications-page-header">
        <h1 className="applications-page-title">Applications Workspace</h1>
        <p className="applications-page-subtitle">
          Interactive Kanban pipeline and dense multi-sort table tracking all internship opportunities.
        </p>
      </header>

      <ApplicationsFilterBar
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        statusFilter={statusFilter}
        onStatusFilterChange={setStatusFilter}
        ratingFilter={ratingFilter}
        onRatingFilterChange={setRatingFilter}
        onClearFilters={handleClearFilters}
        viewMode={viewMode}
        onViewModeChange={setViewMode}
        onAddApplicationClick={handleOpenAddModal}
        filteredCount={sortedApplications.length}
        totalCount={applications.length}
      />

      {isLoading ? (
        <div className="loading-container">
          <Skeleton width="100%" height="60px" style={{ borderRadius: '8px' }} />
          <div style={{ display: 'flex', gap: '16px', width: '100%', marginTop: '16px' }}>
            <Skeleton width="300px" height="360px" style={{ borderRadius: '8px' }} />
            <Skeleton width="300px" height="360px" style={{ borderRadius: '8px' }} />
            <Skeleton width="300px" height="360px" style={{ borderRadius: '8px' }} />
          </div>
        </div>
      ) : error ? (
        <ErrorState
          title="Failed to Load Applications"
          description={error}
          onRetry={fetchApplications}
        />
      ) : applications.length === 0 ? (
        <EmptyState
          title="No Applications Tracked Yet"
          description="Your internship pipeline is empty. Start tracking applications to see your stage conversions, interview schedules, and skill alignment."
          actionLabel="+ Add Your First Application"
          onAction={handleOpenAddModal}
        />
      ) : sortedApplications.length === 0 ? (
        <EmptyState
          title="No Matching Applications"
          description="No applications match your active search and filter criteria. Try broadening your filters."
          actionLabel="Reset Filters"
          onAction={handleClearFilters}
        />
      ) : viewMode === 'kanban' ? (
        <KanbanBoard
          applications={sortedApplications}
          onStatusChange={handleStatusChange}
          onEdit={handleOpenEditModal}
          onDelete={setDeleteTarget}
        />
      ) : (
        <>
          <BatchActionToolbar
            selectedCount={selectedIds.length}
            onBatchUpdateStatus={handleBatchUpdateStatus}
            onDeselectAll={() => setSelectedIds([])}
            isUpdating={isBatchUpdating}
          />
          <ApplicationsTable
            applications={sortedApplications}
            selectedIds={selectedIds}
            onToggleSelect={handleToggleSelect}
            onToggleSelectAll={handleToggleSelectAll}
            sortField={sortField}
            sortOrder={sortOrder}
            onSort={handleSort}
            onStatusChange={handleStatusChange}
            onEdit={handleOpenEditModal}
            onDelete={setDeleteTarget}
          />
        </>
      )}

      <ApplicationModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSubmit={handleSaveModal}
        initialData={modalInitialData}
      />

      <DeleteConfirmModal
        isOpen={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleConfirmDelete}
        application={deleteTarget}
        isDeleting={isDeleting}
      />
    </div>
  );
};