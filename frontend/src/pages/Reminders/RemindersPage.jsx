import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Skeleton } from '../../components/common/Skeleton';
import { EmptyState } from '../../components/states/EmptyState';
import { ErrorState } from '../../components/states/ErrorState';
import { useToast } from '../../context/ToastContext';
import { reminderService } from '../../services/reminderService';
import { applicationService } from '../../services/applicationService';

import { ReminderFilterBar } from './components/ReminderFilterBar';
import { ReminderCard } from './components/ReminderCard';
import { ReminderModal } from './components/ReminderModal';
import { DeleteReminderModal } from './components/DeleteReminderModal';

import './RemindersPage.css';

export const RemindersAndDeadlines = () => {
  const [reminders, setReminders] = useState([]);
  const [applications, setApplications] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [applicationFilter, setApplicationFilter] = useState('ALL');
  const [dueBeforePreset, setDueBeforePreset] = useState('ALL');

  // Compute dueBefore cutoff ISO string based on preset
  const computedDueBefore = useMemo(() => {
    if (dueBeforePreset === 'ALL') return null;
    const now = new Date();
    if (dueBeforePreset === 'TODAY') {
      const endOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59, 999);
      return endOfToday.toISOString();
    }
    if (dueBeforePreset === '24H') {
      return new Date(now.getTime() + 24 * 3600 * 1000).toISOString();
    }
    if (dueBeforePreset === '48H') {
      return new Date(now.getTime() + 48 * 3600 * 1000).toISOString();
    }
    if (dueBeforePreset === '7D') {
      return new Date(now.getTime() + 7 * 24 * 3600 * 1000).toISOString();
    }
    return null;
  }, [dueBeforePreset]);

  // Modals
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalInitialData, setModalInitialData] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const { showToast } = useToast();

  // Load applications once for dropdown metadata
  useEffect(() => {
    let isMounted = true;
    applicationService.getApplications()
      .then((appList) => {
        if (isMounted) setApplications(appList || []);
      })
      .catch(() => {});
    return () => {
      isMounted = false;
    };
  }, []);

  const fetchData = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const params = {};
      if (statusFilter === 'ACTIVE') {
        params.completed = false;
      } else if (statusFilter === 'COMPLETED') {
        params.completed = true;
      }
      if (applicationFilter !== 'ALL') {
        params.applicationId = applicationFilter;
      }
      if (computedDueBefore) {
        params.dueBefore = computedDueBefore;
      }

      const remList = await reminderService.getReminders(params);
      setReminders(remList || []);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to load reminders');
    } finally {
      setIsLoading(false);
    }
  }, [statusFilter, applicationFilter, computedDueBefore]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // Lookup map for fast application metadata matching
  const applicationsById = useMemo(() => {
    const map = {};
    applications.forEach((app) => {
      map[app.id] = app;
    });
    return map;
  }, [applications]);

  // Toggle complete with authoritative state confirmation and rollback on failure
  const handleToggleComplete = async (reminder) => {
    const prevCompleted = reminder.completed;
    const newCompleted = !prevCompleted;

    // Optimistic update
    setReminders((prev) =>
      prev.map((r) => (r.id === reminder.id ? { ...r, completed: newCompleted } : r))
    );

    try {
      if (newCompleted) {
        const updated = await reminderService.completeReminder(reminder.id);
        // Overwrite with authoritative backend response
        setReminders((prev) =>
          prev.map((r) => (r.id === reminder.id ? { ...r, ...updated } : r))
        );
        showToast('Reminder marked as completed', 'success');
      } else {
        const updated = await reminderService.updateReminder(reminder.id, {
          title: reminder.title,
          dueAt: reminder.dueAt,
          completed: false,
          reminderType: reminder.reminderType,
          applicationId: reminder.applicationId,
        });
        setReminders((prev) =>
          prev.map((r) => (r.id === reminder.id ? { ...r, ...updated } : r))
        );
        showToast('Reminder marked as active', 'info');
      }
    } catch (err) {
      // Explicit rollback on API failure: never leave UI completed if PATCH failed
      setReminders((prev) =>
        prev.map((r) => (r.id === reminder.id ? { ...r, completed: prevCompleted } : r))
      );
      showToast(err.response?.data?.message || 'Failed to update reminder status', 'error');
    }
  };

  // Create / Edit modal handler
  const handleSaveReminder = async (payload, isEdit, reminderId) => {
    if (isEdit) {
      const updated = await reminderService.updateReminder(reminderId, payload);
      setReminders((prev) =>
        prev.map((r) => (r.id === reminderId ? { ...r, ...updated } : r))
      );
      showToast('Reminder updated successfully', 'success');
    } else {
      const created = await reminderService.createReminder(payload);
      setReminders((prev) => [...prev, created]);
      showToast('Reminder created successfully', 'success');
    }
  };

  // Delete modal handler
  const handleDeleteReminder = async () => {
    if (!deleteTarget) return;
    setIsDeleting(true);
    try {
      await reminderService.deleteReminder(deleteTarget.id);
      setReminders((prev) => prev.filter((r) => r.id !== deleteTarget.id));
      showToast('Reminder deleted successfully', 'success');
      setDeleteTarget(null);
    } catch (err) {
      showToast(err.response?.data?.message || 'Failed to delete reminder', 'error');
    } finally {
      setIsDeleting(false);
    }
  };

  // Filtered dataset: backend already handled status, application, and dueBefore filters
  const filteredList = useMemo(() => {
    if (!searchQuery.trim()) {
      return reminders;
    }
    const q = searchQuery.toLowerCase();
    return reminders.filter((r) => {
      const app = applicationsById[r.applicationId];
      const comp = (app?.companyName || '').toLowerCase();
      const role = (app?.opportunityTitle || '').toLowerCase();
      const title = (r.title || '').toLowerCase();
      return title.includes(q) || comp.includes(q) || role.includes(q);
    });
  }, [reminders, searchQuery, applicationsById]);

  // Grouping into local calendar urgency buckets
  const urgencyGroups = useMemo(() => {
    const now = new Date();
    const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0, 0).getTime();
    const endOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59, 999).getTime();
    const endOfWeek = endOfToday + (7 - now.getDay()) * 24 * 3600 * 1000;

    const overdue = [];
    const dueToday = [];
    const thisWeek = [];
    const later = [];
    const completed = [];

    filteredList.forEach((r) => {
      if (r.completed) {
        completed.push(r);
        return;
      }

      const dueTime = r.dueAt ? new Date(r.dueAt).getTime() : null;

      if (dueTime === null) {
        later.push(r);
      } else if (dueTime < startOfToday) {
        overdue.push(r);
      } else if (dueTime >= startOfToday && dueTime <= endOfToday) {
        dueToday.push(r);
      } else if (dueTime > endOfToday && dueTime <= endOfWeek) {
        thisWeek.push(r);
      } else {
        later.push(r);
      }
    });

    // Sort within buckets
    overdue.sort((a, b) => new Date(a.dueAt || 0) - new Date(b.dueAt || 0));
    dueToday.sort((a, b) => new Date(a.dueAt || 0) - new Date(b.dueAt || 0));
    thisWeek.sort((a, b) => new Date(a.dueAt || 0) - new Date(b.dueAt || 0));
    later.sort((a, b) => new Date(a.dueAt || 0) - new Date(b.dueAt || 0));
    completed.sort((a, b) => new Date(b.dueAt || 0) - new Date(a.dueAt || 0));

    return { overdue, dueToday, thisWeek, later, completed };
  }, [filteredList]);

  return (
    <div className="reminders-page">
      <header className="reminders-page-header">
        <h1 className="reminders-page-title">Reminders & Deadlines</h1>
        <p className="reminders-page-subtitle">
          Online assessments, application follow-ups, and interview preparation milestone tasks.
        </p>
      </header>

      <ReminderFilterBar
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        statusFilter={statusFilter}
        onStatusFilterChange={setStatusFilter}
        applicationFilter={applicationFilter}
        onApplicationFilterChange={setApplicationFilter}
        dueBeforePreset={dueBeforePreset}
        onDueBeforePresetChange={setDueBeforePreset}
        applications={applications}
        onCreateClick={() => {
          setModalInitialData(null);
          setIsModalOpen(true);
        }}
      />

      {isLoading ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
          <Skeleton width="100%" height="48px" style={{ borderRadius: '8px' }} />
          <Skeleton width="100%" height="48px" style={{ borderRadius: '8px' }} />
          <Skeleton width="100%" height="48px" style={{ borderRadius: '8px' }} />
        </div>
      ) : error ? (
        <ErrorState
          title="Failed to Load Reminders"
          description={error}
          onRetry={fetchData}
        />
      ) : reminders.length === 0 ? (
        <EmptyState
          title="No Reminders Yet"
          description="Track OA deadlines, interview prep milestones, and application follow-ups with scheduled reminders."
          actionLabel="+ Create Your First Reminder"
          onAction={() => {
            setModalInitialData(null);
            setIsModalOpen(true);
          }}
        />
      ) : filteredList.length === 0 ? (
        <EmptyState
          title="No Matching Reminders"
          description="No tasks match your active filter criteria. Try changing your filters."
          actionLabel="Reset Filters"
          onAction={() => {
            setSearchQuery('');
            setStatusFilter('ALL');
            setApplicationFilter('ALL');
            setDueBeforePreset('ALL');
          }}
        />
      ) : (
        <div>
          {/* Overdue */}
          {urgencyGroups.overdue.length > 0 && (
            <section className="urgency-section">
              <div className="urgency-section-header">
                <span className="urgency-section-title" style={{ color: '#f87171' }}>Overdue</span>
                <span className="urgency-count-badge overdue">{urgencyGroups.overdue.length}</span>
              </div>
              {urgencyGroups.overdue.map((r) => (
                <ReminderCard
                  key={r.id}
                  reminder={r}
                  application={applicationsById[r.applicationId]}
                  onToggleComplete={handleToggleComplete}
                  onEdit={(item) => {
                    setModalInitialData(item);
                    setIsModalOpen(true);
                  }}
                  onDelete={(item) => setDeleteTarget(item)}
                />
              ))}
            </section>
          )}

          {/* Due Today */}
          {urgencyGroups.dueToday.length > 0 && (
            <section className="urgency-section">
              <div className="urgency-section-header">
                <span className="urgency-section-title" style={{ color: '#fbbf24' }}>Due Today</span>
                <span className="urgency-count-badge today">{urgencyGroups.dueToday.length}</span>
              </div>
              {urgencyGroups.dueToday.map((r) => (
                <ReminderCard
                  key={r.id}
                  reminder={r}
                  application={applicationsById[r.applicationId]}
                  onToggleComplete={handleToggleComplete}
                  onEdit={(item) => {
                    setModalInitialData(item);
                    setIsModalOpen(true);
                  }}
                  onDelete={(item) => setDeleteTarget(item)}
                />
              ))}
            </section>
          )}

          {/* This Week */}
          {urgencyGroups.thisWeek.length > 0 && (
            <section className="urgency-section">
              <div className="urgency-section-header">
                <span className="urgency-section-title" style={{ color: '#60a5fa' }}>This Week</span>
                <span className="urgency-count-badge week">{urgencyGroups.thisWeek.length}</span>
              </div>
              {urgencyGroups.thisWeek.map((r) => (
                <ReminderCard
                  key={r.id}
                  reminder={r}
                  application={applicationsById[r.applicationId]}
                  onToggleComplete={handleToggleComplete}
                  onEdit={(item) => {
                    setModalInitialData(item);
                    setIsModalOpen(true);
                  }}
                  onDelete={(item) => setDeleteTarget(item)}
                />
              ))}
            </section>
          )}

          {/* Later */}
          {urgencyGroups.later.length > 0 && (
            <section className="urgency-section">
              <div className="urgency-section-header">
                <span className="urgency-section-title" style={{ color: 'var(--color-text-secondary)' }}>Later</span>
                <span className="urgency-count-badge later">{urgencyGroups.later.length}</span>
              </div>
              {urgencyGroups.later.map((r) => (
                <ReminderCard
                  key={r.id}
                  reminder={r}
                  application={applicationsById[r.applicationId]}
                  onToggleComplete={handleToggleComplete}
                  onEdit={(item) => {
                    setModalInitialData(item);
                    setIsModalOpen(true);
                  }}
                  onDelete={(item) => setDeleteTarget(item)}
                />
              ))}
            </section>
          )}

          {/* Completed */}
          {urgencyGroups.completed.length > 0 && (
            <section className="urgency-section">
              <div className="urgency-section-header">
                <span className="urgency-section-title" style={{ color: '#34d399' }}>Completed</span>
                <span className="urgency-count-badge completed">{urgencyGroups.completed.length}</span>
              </div>
              {urgencyGroups.completed.map((r) => (
                <ReminderCard
                  key={r.id}
                  reminder={r}
                  application={applicationsById[r.applicationId]}
                  onToggleComplete={handleToggleComplete}
                  onEdit={(item) => {
                    setModalInitialData(item);
                    setIsModalOpen(true);
                  }}
                  onDelete={(item) => setDeleteTarget(item)}
                />
              ))}
            </section>
          )}
        </div>
      )}

      {/* Create / Edit Modal */}
      <ReminderModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSave={handleSaveReminder}
        initialData={modalInitialData}
        applications={applications}
      />

      {/* Delete Confirmation Modal */}
      <DeleteReminderModal
        isOpen={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDeleteReminder}
        reminder={deleteTarget}
        isDeleting={isDeleting}
      />
    </div>
  );
};

export const RemindersPage = RemindersAndDeadlines;
export default RemindersAndDeadlines;
