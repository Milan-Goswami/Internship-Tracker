import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Skeleton } from '../../components/common/Skeleton';
import { ErrorState } from '../../components/states/ErrorState';
import { useToast } from '../../context/ToastContext';
import { analyticsService } from '../../services/analyticsService';
import { reminderService } from '../../services/reminderService';

import { ActionHubBanner } from './components/ActionHubBanner';
import { MetricCard } from './components/MetricCard';
import { UpcomingInterviewsWidget } from './components/UpcomingInterviewsWidget';
import { PipelineFunnelWidget } from './components/PipelineFunnelWidget';
import { ActiveRemindersWidget } from './components/ActiveRemindersWidget';
import { StatusDistributionWidget } from './components/StatusDistributionWidget';

import './DashboardPage.css';

export const CommandCenterDashboard = () => {
  const [summary, setSummary] = useState(null);
  const [funnel, setFunnel] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const { showToast } = useToast();

  const fetchData = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [sumData, funData] = await Promise.all([
        analyticsService.getDashboardSummary(),
        analyticsService.getFunnelAnalytics(),
      ]);
      setSummary(sumData);
      setFunnel(funData);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to load dashboard analytics');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // Action Hub items: OA deadlines <24h and interviews <48h
  const urgentActionItems = useMemo(() => {
    if (!summary) return [];
    const items = [];
    const now = Date.now();

    // 1. OA deadlines < 24h
    (summary.urgentReminders || []).forEach((r) => {
      if (!r.completed && r.reminderType === 'OA_DEADLINE' && r.dueAt) {
        const diffMs = new Date(r.dueAt).getTime() - now;
        if (diffMs > 0 && diffMs < 24 * 3600 * 1000) {
          items.push({
            id: `oa-${r.id}`,
            title: `Online Assessment Due Soon: "${r.title}"`,
            actionLabel: 'Go to Reminders',
            link: '/reminders',
          });
        }
      }
    });

    // 2. Scheduled interviews < 48h
    (summary.upcomingInterviews || []).forEach((i) => {
      if (i.scheduledAt) {
        const diffMs = new Date(i.scheduledAt).getTime() - now;
        if (diffMs > 0 && diffMs < 48 * 3600 * 1000) {
          items.push({
            id: `interview-${i.id}`,
            title: `Interview Scheduled in <48h: ${i.roundTitle} with ${i.companyName || 'Company'}`,
            actionLabel: 'Open Interview Hub',
            link: '/interviews',
          });
        }
      }
    });

    return items;
  }, [summary]);

  // 1-click completion directly from dashboard with rollback on failure
  const handleToggleReminderComplete = async (reminder) => {
    if (!summary) return;

    const prevReminders = summary.urgentReminders || [];
    const prevCompleted = reminder.completed;
    const newCompleted = !prevCompleted;

    // Granular local state update: zero refetch of the entire dashboard analytics
    setSummary((prev) => ({
      ...prev,
      urgentReminders: prev.urgentReminders.map((r) =>
        r.id === reminder.id ? { ...r, completed: newCompleted } : r
      ),
    }));

    try {
      if (newCompleted) {
        const updated = await reminderService.completeReminder(reminder.id);
        setSummary((prev) => ({
          ...prev,
          urgentReminders: prev.urgentReminders.map((r) =>
            r.id === reminder.id ? { ...r, ...updated } : r
          ),
        }));
        showToast('Reminder completed', 'success');
      } else {
        const updated = await reminderService.updateReminder(reminder.id, {
          title: reminder.title,
          dueAt: reminder.dueAt,
          completed: false,
          reminderType: reminder.reminderType,
          applicationId: reminder.applicationId,
        });
        setSummary((prev) => ({
          ...prev,
          urgentReminders: prev.urgentReminders.map((r) =>
            r.id === reminder.id ? { ...r, ...updated } : r
          ),
        }));
        showToast('Reminder active', 'info');
      }
    } catch (err) {
      // Explicit rollback on API failure
      setSummary((prev) => ({
        ...prev,
        urgentReminders: prevReminders,
      }));
      showToast(err.response?.data?.message || 'Failed to update reminder', 'error');
    }
  };

  return (
    <div className="dashboard-page">
      <header className="dashboard-header">
        <h1 className="dashboard-title">Command Center</h1>
        <p className="dashboard-subtitle">
          Real-time pipeline intelligence, upcoming interview schedule, and urgent deadline tracking.
        </p>
      </header>

      {/* Action Hub Urgent Banner (<24h OA, <48h Interview) */}
      <ActionHubBanner urgentItems={urgentActionItems} />

      {isLoading ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
          <div className="kpi-grid">
            <Skeleton width="100%" height="110px" style={{ borderRadius: '12px' }} />
            <Skeleton width="100%" height="110px" style={{ borderRadius: '12px' }} />
            <Skeleton width="100%" height="110px" style={{ borderRadius: '12px' }} />
            <Skeleton width="100%" height="110px" style={{ borderRadius: '12px' }} />
          </div>
          <div className="dashboard-main-grid">
            <Skeleton width="100%" height="280px" style={{ borderRadius: '12px' }} />
            <Skeleton width="100%" height="280px" style={{ borderRadius: '12px' }} />
          </div>
        </div>
      ) : error ? (
        <ErrorState
          title="Failed to Load Dashboard Analytics"
          description={error}
          onRetry={fetchData}
        />
      ) : (
        <>
          {/* 4 Metric Quick-Cards */}
          <div className="kpi-grid">
            <MetricCard
              label="Active Applications"
              value={summary?.activeApplications ?? 0}
              subtext={`${summary?.totalApplications ?? 0} Total in Pipeline`}
              icon={
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                  <polyline points="14 2 14 8 20 8" />
                </svg>
              }
            />

            <MetricCard
              label="Interview Rate"
              value={`${summary?.interviewRate ?? 0}%`}
              subtext="Interviewing / Applied Ratio"
              iconBg="rgba(59, 130, 246, 0.15)"
              iconColor="#60a5fa"
              icon={
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
                </svg>
              }
            />

            <MetricCard
              label="Pending OAs"
              value={summary?.pendingOAs ?? 0}
              subtext="Incomplete OA Deadlines"
              iconBg="rgba(245, 158, 11, 0.15)"
              iconColor="#fbbf24"
              icon={
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="12" cy="12" r="10" />
                  <polyline points="12 6 12 12 16 14" />
                </svg>
              }
            />

            <MetricCard
              label="Offers Received"
              value={summary?.offersReceived ?? 0}
              subtext="Offer / Accepted Status"
              iconBg="rgba(52, 211, 153, 0.15)"
              iconColor="#34d399"
              icon={
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="12" cy="8" r="7" />
                  <polyline points="8.21 13.89 7 23 12 20 17 23 15.79 13.88" />
                </svg>
              }
            />
          </div>

          {/* Main Dashboard Widget Grid */}
          <div className="dashboard-main-grid">
            <UpcomingInterviewsWidget
              interviews={summary?.upcomingInterviews || []}
            />

            <PipelineFunnelWidget
              funnel={funnel}
            />

            <ActiveRemindersWidget
              reminders={summary?.urgentReminders || []}
              onToggleComplete={handleToggleReminderComplete}
            />

            <StatusDistributionWidget
              statusCounts={summary?.statusCounts || {}}
            />
          </div>
        </>
      )}
    </div>
  );
};

export const DashboardPage = CommandCenterDashboard;
export default CommandCenterDashboard;
