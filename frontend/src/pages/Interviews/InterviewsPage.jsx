import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { Skeleton } from '../../components/common/Skeleton';
import { EmptyState } from '../../components/states/EmptyState';
import { ErrorState } from '../../components/states/ErrorState';
import { useToast } from '../../context/ToastContext';
import { applicationService } from '../../services/applicationService';
import { interviewService } from '../../services/interviewService';

import { InterviewFilterBar } from './components/InterviewFilterBar';
import { InterviewCard } from './components/InterviewCard';
import { InterviewPrepDrawer } from './components/InterviewPrepDrawer';
import { InterviewModal } from './components/InterviewModal';
import { DeleteInterviewModal } from './components/DeleteInterviewModal';

import './InterviewsPage.css';

export const InterviewHub = () => {
  const navigate = useNavigate();

  // Canonical state
  const [interviews, setInterviews] = useState([]);
  const [applications, setApplications] = useState([]);

  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [applicationFilter, setApplicationFilter] = useState('ALL');
  const [outcomeFilter, setOutcomeFilter] = useState('ALL');
  const [roundTypeFilter, setRoundTypeFilter] = useState('ALL');

  // Slide-over drawer: track by ID to guarantee single source of truth
  const [drawerInterviewId, setDrawerInterviewId] = useState(null);

  // Modals
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalInitialData, setModalInitialData] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Single page-level live timer tick (1s) to update countdowns and upcoming/completed transitions
  const [currentTime, setCurrentTime] = useState(() => Date.now());

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(Date.now());
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const { showToast } = useToast();

  // Parallel fetch with deduplication and safe failure isolation
  const fetchData = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const apps = await applicationService.getApplications();
      const appsList = apps || [];
      setApplications(appsList);

      if (appsList.length === 0) {
        setInterviews([]);
        setIsLoading(false);
        return;
      }

      // Deduplicate application IDs
      const uniqueAppIds = Array.from(new Set(appsList.map((a) => a.id)));

      // Fetch interviews in parallel using allSettled
      const settledResults = await Promise.allSettled(
        uniqueAppIds.map((id) => interviewService.getInterviewsByApplication(id))
      );

      const allInterviews = [];
      settledResults.forEach((result) => {
        if (result.status === 'fulfilled' && Array.isArray(result.value)) {
          allInterviews.push(...result.value);
        }
      });

      setInterviews(allInterviews);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to load interviews');
    } finally {
      setIsLoading(false);
    }
  }, []);

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

  // Client-side filtering and chronological sorting
  const filteredInterviews = useMemo(() => {
    const list = interviews.filter((interview) => {
      // 1. Application Filter
      if (applicationFilter !== 'ALL' && String(interview.applicationId) !== String(applicationFilter)) {
        return false;
      }

      // 2. Outcome Filter
      if (outcomeFilter !== 'ALL' && (interview.outcome || 'PENDING') !== outcomeFilter) {
        return false;
      }

      // 3. Round Type Filter
      if (roundTypeFilter !== 'ALL' && interview.roundType !== roundTypeFilter) {
        return false;
      }

      // 4. Text Search (title, interviewer names, prep notes, company, role)
      if (searchQuery.trim()) {
        const query = searchQuery.toLowerCase();
        const app = applicationsById[interview.applicationId];
        const comp = (app?.companyName || '').toLowerCase();
        const role = (app?.opportunityTitle || '').toLowerCase();
        const title = (interview.roundTitle || '').toLowerCase();
        const interviewer = (interview.interviewerNames || '').toLowerCase();
        const notes = (interview.prepNotes || '').toLowerCase();

        if (
          !title.includes(query) &&
          !interviewer.includes(query) &&
          !notes.includes(query) &&
          !comp.includes(query) &&
          !role.includes(query)
        ) {
          return false;
        }
      }

      return true;
    });

    // Partition into upcoming, completed, and undated
    const upcoming = [];
    const completed = [];
    const undated = [];

    const parseTime = (isoString) => {
      if (!isoString) return null;
      const t = new Date(isoString).getTime();
      return isNaN(t) ? null : t;
    };

    for (const interview of list) {
      const time = parseTime(interview.scheduledAt);
      if (time === null) {
        undated.push(interview);
      } else if (time > currentTime) {
        upcoming.push({ interview, time });
      } else {
        completed.push({ interview, time });
      }
    }

    // 1. Upcoming interviews first, sorted earliest -> latest
    upcoming.sort((a, b) => a.time - b.time || (a.interview.id || 0) - (b.interview.id || 0));

    // 2. Completed interviews after upcoming interviews, sorted most recent -> oldest
    completed.sort((a, b) => b.time - a.time || (b.interview.id || 0) - (a.interview.id || 0));

    // 3. Undated interviews at the end, deterministically placed
    undated.sort((a, b) => (a.id || 0) - (b.id || 0));

    return [
      ...upcoming.map((u) => u.interview),
      ...completed.map((c) => c.interview),
      ...undated,
    ];
  }, [interviews, applicationsById, applicationFilter, outcomeFilter, roundTypeFilter, searchQuery, currentTime]);

  // Clear filters
  const handleClearFilters = () => {
    setSearchQuery('');
    setApplicationFilter('ALL');
    setOutcomeFilter('ALL');
    setRoundTypeFilter('ALL');
  };

  // Unified canonical outcome recorder
  const handleRecordOutcome = async (interviewId, outcome, reflectionNotes) => {
    try {
      const updated = await interviewService.recordOutcome(interviewId, {
        outcome,
        reflectionNotes,
      });
      // Update in-place in canonical state: guarantees all components reflect confirmed state
      setInterviews((prev) =>
        prev.map((item) => (item.id === interviewId ? { ...item, ...updated } : item))
      );
      showToast(`Interview marked as ${outcome}`, 'success');
    } catch (err) {
      showToast(err.response?.data?.message || 'Failed to record outcome', 'error');
    }
  };

  // Create / Edit modal handlers
  const handleOpenScheduleModal = () => {
    setModalInitialData(null);
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (interview) => {
    setModalInitialData(interview);
    setIsModalOpen(true);
  };

  const handleSaveInterview = async (appId, payload, isEdit, interviewId) => {
    if (isEdit) {
      const updated = await interviewService.updateInterview(interviewId, payload);
      setInterviews((prev) =>
        prev.map((item) => (item.id === interviewId ? { ...item, ...updated } : item))
      );
      showToast('Interview round updated successfully', 'success');
    } else {
      const created = await interviewService.createInterview(appId, payload);
      // In-place append: zero refetch of other applications
      setInterviews((prev) => [...prev, created]);
      showToast(`Scheduled Round ${created.roundNumber}: ${created.roundTitle}`, 'success');
    }
  };

  // Delete interview round
  const handleDeleteInterview = async () => {
    if (!deleteTarget) return;
    setIsDeleting(true);
    try {
      await interviewService.deleteInterview(deleteTarget.id);
      // In-place filter: zero refetch of other applications
      setInterviews((prev) => prev.filter((item) => item.id !== deleteTarget.id));
      if (drawerInterviewId === deleteTarget.id) {
        setDrawerInterviewId(null);
      }
      showToast('Interview round deleted successfully', 'success');
      setDeleteTarget(null);
    } catch (err) {
      showToast(err.response?.data?.message || 'Failed to delete interview round', 'error');
    } finally {
      setIsDeleting(false);
    }
  };

  // Active drawer interview resolved directly from canonical state
  const activeDrawerInterview = useMemo(() => {
    if (!drawerInterviewId) return null;
    return interviews.find((i) => i.id === drawerInterviewId) || null;
  }, [drawerInterviewId, interviews]);

  return (
    <div className="interviews-page">
      <header className="interviews-page-header">
        <h1 className="interviews-page-title">Interview Hub</h1>
        <p className="interviews-page-subtitle">
          Multi-round schedule timeline, live countdowns, technical preparation notes, and interview reflection tracking.
        </p>
      </header>

      <InterviewFilterBar
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        applicationFilter={applicationFilter}
        onApplicationFilterChange={setApplicationFilter}
        outcomeFilter={outcomeFilter}
        onOutcomeFilterChange={setOutcomeFilter}
        roundTypeFilter={roundTypeFilter}
        onRoundTypeFilterChange={setRoundTypeFilter}
        onClearFilters={handleClearFilters}
        onScheduleClick={handleOpenScheduleModal}
        applications={applications}
        filteredCount={filteredInterviews.length}
        totalCount={interviews.length}
      />

      {isLoading ? (
        <div className="interviews-loading-container">
          <div className="interviews-loading-grid">
            <Skeleton width="100%" height="240px" style={{ borderRadius: '12px' }} />
            <Skeleton width="100%" height="240px" style={{ borderRadius: '12px' }} />
            <Skeleton width="100%" height="240px" style={{ borderRadius: '12px' }} />
          </div>
        </div>
      ) : error ? (
        <ErrorState
          title="Failed to Load Interviews"
          description={error}
          onRetry={fetchData}
        />
      ) : applications.length === 0 ? (
        <EmptyState
          title="Track an Application First"
          description="Interviews are linked directly to your application lifecycle records. Add an application to your pipeline before scheduling interview rounds."
          actionLabel="Go to Applications Workspace"
          onAction={() => navigate('/applications')}
        />
      ) : interviews.length === 0 ? (
        <EmptyState
          title="No Interviews Scheduled Yet"
          description="You have active applications, but no interview rounds have been logged. Log your first recruiter screen or technical assessment."
          actionLabel="+ Schedule Your First Interview"
          onAction={handleOpenScheduleModal}
        />
      ) : filteredInterviews.length === 0 ? (
        <EmptyState
          title="No Matching Interviews"
          description="No interview rounds match your active filter criteria. Try resetting your filters."
          actionLabel="Reset Filters"
          onAction={handleClearFilters}
        />
      ) : (
        <div className="interviews-timeline-grid">
          {filteredInterviews.map((interview) => (
            <InterviewCard
              key={interview.id}
              interview={interview}
              application={applicationsById[interview.applicationId]}
              now={currentTime}
              onOpenDrawer={(item) => setDrawerInterviewId(item.id)}
              onRecordOutcome={handleRecordOutcome}
              onEdit={handleOpenEditModal}
              onDelete={(item) => setDeleteTarget(item)}
            />
          ))}
        </div>
      )}

      {/* Slide-over Prep Notes & Reflection Drawer */}
      <InterviewPrepDrawer
        interview={activeDrawerInterview}
        application={activeDrawerInterview ? applicationsById[activeDrawerInterview.applicationId] : null}
        isOpen={Boolean(activeDrawerInterview)}
        onClose={() => setDrawerInterviewId(null)}
        onRecordOutcome={handleRecordOutcome}
        onEdit={handleOpenEditModal}
      />

      {/* Schedule / Edit Interview Modal */}
      <InterviewModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSave={handleSaveInterview}
        initialData={modalInitialData}
        applications={applications}
        existingInterviews={interviews}
      />

      {/* Delete Interview Confirmation Modal */}
      <DeleteInterviewModal
        isOpen={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDeleteInterview}
        interview={deleteTarget}
        isDeleting={isDeleting}
      />
    </div>
  );
};

export const InterviewsPage = InterviewHub;
export default InterviewHub;
