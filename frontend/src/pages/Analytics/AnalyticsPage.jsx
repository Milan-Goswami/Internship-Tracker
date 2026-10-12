import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Skeleton } from '../../components/common/Skeleton';
import { Button } from '../../components/common/Button';
import { ErrorState } from '../../components/states/ErrorState';
import { EmptyState } from '../../components/states/EmptyState';
import { useToast } from '../../context/ToastContext';
import { analyticsService } from '../../services/analyticsService';
import { ConversionMetricsGrid } from './components/ConversionMetricsGrid';
import { FunnelPipelineChart } from './components/FunnelPipelineChart';
import { StatusBreakdownCard } from './components/StatusBreakdownCard';
import './AnalyticsPage.css';

export const FunnelAnalyticsAndIntelligence = () => {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [funnel, setFunnel] = useState(null);
  const [summary, setSummary] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchData = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [funnelRes, summaryRes] = await Promise.all([
        analyticsService.getFunnelAnalytics(),
        analyticsService.getDashboardSummary(),
      ]);
      setFunnel(funnelRes);
      setSummary(summaryRes);
    } catch (err) {
      console.error('Analytics load error:', err);
      setError(err.response?.data?.message || err.message || 'Failed to load funnel analytics');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const hasPipelineData = Boolean(
    (funnel?.totalSaved && funnel.totalSaved > 0) ||
    (funnel?.totalApplied && funnel.totalApplied > 0) ||
    (summary?.totalApplications && summary.totalApplications > 0)
  );

  return (
    <div className="analytics-page">
      <header className="analytics-page-header">
        <div>
          <h1 className="analytics-page-title">Funnel Analytics & Intelligence</h1>
          <p className="analytics-page-subtitle">
            Deterministic stage conversion ratios and pipeline velocity based on first-entry stage timestamps.
          </p>
        </div>
        <div className="analytics-header-actions">
          <Button
            variant="secondary"
            onClick={() => {
              fetchData();
              showToast('Analytics refreshed', 'info');
            }}
            disabled={isLoading}
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M23 4v6h-6" />
              <path d="M1 20v-6h6" />
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
            </svg>
            Refresh
          </Button>
          <Button
            variant="primary"
            onClick={() => navigate('/applications')}
          >
            Manage Pipeline
          </Button>
        </div>
      </header>

      {isLoading ? (
        <div className="analytics-loading-container">
          <div className="analytics-metrics-grid">
            <Skeleton width="100%" height="110px" style={{ borderRadius: '12px' }} />
            <Skeleton width="100%" height="110px" style={{ borderRadius: '12px' }} />
            <Skeleton width="100%" height="110px" style={{ borderRadius: '12px' }} />
            <Skeleton width="100%" height="110px" style={{ borderRadius: '12px' }} />
          </div>
          <Skeleton width="100%" height="340px" style={{ borderRadius: '12px', marginTop: 'var(--space-6)' }} />
          <Skeleton width="100%" height="220px" style={{ borderRadius: '12px', marginTop: 'var(--space-6)' }} />
        </div>
      ) : error ? (
        <ErrorState
          title="Failed to Load Analytics"
          description={error}
          onRetry={fetchData}
        />
      ) : !hasPipelineData ? (
        <EmptyState
          title="No Application Pipeline Activity Yet"
          description="Conversion analytics are computed dynamically as you save roles, submit applications, and progress through interview rounds. Add your first application to unlock conversion insights."
          actionLabel="+ Track Your First Application"
          onAction={() => navigate('/applications')}
        />
      ) : (
        <div className="analytics-content">
          {/* Top Row: Conversion Velocity KPIs */}
          <ConversionMetricsGrid funnel={funnel} />

          {/* Middle Row: Visual Conversion Funnel */}
          <FunnelPipelineChart funnel={funnel} />

          {/* Bottom Row: Status Distribution Card */}
          <StatusBreakdownCard
            statusCounts={summary?.statusCounts || {}}
            totalApplications={summary?.totalApplications || 0}
          />

          {/* Quick-Nav Insight Banner */}
          <div className="analytics-insight-banner">
            <div className="insight-banner-left">
              <div className="insight-badge">Skill Gap Intelligence</div>
              <h3 className="insight-title">Missing skills across your pipeline?</h3>
              <p className="insight-desc">
                Review top requested technical requirements across your opportunities to prioritize skills and close deficits.
              </p>
            </div>
            <Button variant="secondary" onClick={() => navigate('/skills')}>
              View Skill Matrix
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <polyline points="9 18 15 12 9 6" />
              </svg>
            </Button>
          </div>
        </div>
      )}
    </div>
  );
};

export const AnalyticsPage = FunnelAnalyticsAndIntelligence;
export default FunnelAnalyticsAndIntelligence;