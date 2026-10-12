import React from 'react';
import { Button } from '../../../components/common/Button';
import './InterviewFilterBar.css';

const ROUND_TYPES = [
  { value: 'ALL', label: 'All Round Types' },
  { value: 'RECRUITER_SCREEN', label: 'Recruiter Screen' },
  { value: 'TECHNICAL_OA', label: 'Technical OA' },
  { value: 'LIVE_CODING', label: 'Live Coding' },
  { value: 'SYSTEM_DESIGN', label: 'System Design' },
  { value: 'BEHAVIORAL', label: 'Behavioral' },
  { value: 'EXECUTIVE_FINAL', label: 'Executive Final' },
];

const OUTCOMES = [
  { value: 'ALL', label: 'All Outcomes' },
  { value: 'PENDING', label: 'Upcoming / Pending' },
  { value: 'PASSED', label: 'Passed' },
  { value: 'FAILED', label: 'Failed' },
  { value: 'RESCHEDULED', label: 'Rescheduled' },
];

export const InterviewFilterBar = ({
  searchQuery,
  onSearchChange,
  applicationFilter,
  onApplicationFilterChange,
  outcomeFilter,
  onOutcomeFilterChange,
  roundTypeFilter,
  onRoundTypeFilterChange,
  onClearFilters,
  onScheduleClick,
  applications = [],
  filteredCount,
  totalCount,
}) => {
  const isFiltered =
    Boolean(searchQuery.trim()) ||
    applicationFilter !== 'ALL' ||
    outcomeFilter !== 'ALL' ||
    roundTypeFilter !== 'ALL';

  return (
    <div className="interview-filter-bar">
      <div className="interview-filter-top">
        <div className="interview-search-wrapper">
          <svg
            className="interview-search-icon"
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
          >
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            type="text"
            className="interview-search-input"
            placeholder="Search by interviewer, round title, notes, company, or role..."
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
            aria-label="Search interviews"
          />
          {searchQuery && (
            <button
              type="button"
              className="interview-search-clear"
              onClick={() => onSearchChange('')}
              aria-label="Clear search"
            >
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          )}
        </div>

        <Button variant="primary" size="md" onClick={onScheduleClick}>
          + Schedule Interview Round
        </Button>
      </div>

      <div className="interview-filter-bottom">
        <div className="interview-filter-groups">
          <select
            className="interview-select"
            value={applicationFilter}
            onChange={(e) => onApplicationFilterChange(e.target.value)}
            aria-label="Filter by Application"
          >
            <option value="ALL">All Applications</option>
            {applications.map((app) => (
              <option key={app.id} value={app.id}>
                {app.companyName || 'Company'} — {app.opportunityTitle || 'Role'}
              </option>
            ))}
          </select>

          <select
            className="interview-select"
            value={outcomeFilter}
            onChange={(e) => onOutcomeFilterChange(e.target.value)}
            aria-label="Filter by Outcome"
          >
            {OUTCOMES.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>

          <select
            className="interview-select"
            value={roundTypeFilter}
            onChange={(e) => onRoundTypeFilterChange(e.target.value)}
            aria-label="Filter by Round Type"
          >
            {ROUND_TYPES.map((rt) => (
              <option key={rt.value} value={rt.value}>
                {rt.label}
              </option>
            ))}
          </select>
        </div>

        <div className="interview-filter-meta">
          <span className="interview-count">
            Showing {filteredCount} of {totalCount} rounds
          </span>
          {isFiltered && (
            <button
              type="button"
              className="interview-clear-btn"
              onClick={onClearFilters}
            >
              Reset Filters
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
