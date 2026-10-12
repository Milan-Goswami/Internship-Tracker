import React from 'react';
import { Button } from '../../../components/common/Button';
import './ApplicationsFilterBar.css';

export const ApplicationsFilterBar = ({
  searchQuery,
  onSearchChange,
  statusFilter,
  onStatusFilterChange,
  ratingFilter,
  onRatingFilterChange,
  onClearFilters,
  viewMode,
  onViewModeChange,
  onAddApplicationClick,
  filteredCount,
  totalCount,
}) => {
  const hasActiveFilters = searchQuery !== '' || statusFilter !== 'ALL' || ratingFilter !== 'ALL';

  return (
    <div className="applications-filter-bar">
      <div className="filter-bar-top">
        <div className="filter-bar-search-wrapper">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            type="text"
            className="filter-bar-search-input input"
            placeholder="Search by company, role, or notes..."
            aria-label="Search applications by company, role, or notes"
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
          />
        </div>

        <div className="filter-bar-actions">
          <div className="view-mode-toggle">
            <button
              className={`view-mode-btn ${viewMode === 'kanban' ? 'active' : ''}`}
              onClick={() => onViewModeChange('kanban')}
              title="Kanban Pipeline Board"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="3" y="3" width="7" height="18" rx="1" />
                <rect x="14" y="3" width="7" height="10" rx="1" />
              </svg>
              Kanban
            </button>
            <button
              className={`view-mode-btn ${viewMode === 'table' ? 'active' : ''}`}
              onClick={() => onViewModeChange('table')}
              title="Dense Data Table"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="3" y1="6" x2="21" y2="6" />
                <line x1="3" y1="12" x2="21" y2="12" />
                <line x1="3" y1="18" x2="21" y2="18" />
              </svg>
              Table
            </button>
          </div>

          <Button
            variant="primary"
            size="md"
            onClick={onAddApplicationClick}
            title="Add Application (Shortcut: n)"
            aria-keyshortcuts="n"
            icon={
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="12" y1="5" x2="12" y2="19" />
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
            }
          >
            Add Application
          </Button>
        </div>
      </div>

      <div className="filter-bar-bottom">
        <div className="filter-selects-group">
          <select
            className="filter-select"
            aria-label="Filter applications by stage"
            value={statusFilter}
            onChange={(e) => onStatusFilterChange(e.target.value)}
          >
            <option value="ALL">All Stages</option>
            <option value="SAVED">Saved</option>
            <option value="APPLIED">Applied</option>
            <option value="SCREENING">Screening</option>
            <option value="INTERVIEWING">Interviewing</option>
            <option value="OFFER">Offer</option>
            <option value="ACCEPTED">Accepted</option>
            <option value="REJECTED">Rejected</option>
            <option value="WITHDRAWN">Withdrawn</option>
          </select>

          <select
            className="filter-select"
            aria-label="Filter applications by rating"
            value={ratingFilter}
            onChange={(e) => onRatingFilterChange(e.target.value)}
          >
            <option value="ALL">All Ratings</option>
            <option value="5">5 Stars</option>
            <option value="4+">4+ Stars</option>
            <option value="3+">3+ Stars</option>
          </select>

          {hasActiveFilters && (
            <button className="clear-filters-btn" onClick={onClearFilters}>
              Reset Filters
            </button>
          )}
        </div>

        <div className="filter-count-badge">
          Showing {filteredCount} of {totalCount} applications
        </div>
      </div>
    </div>
  );
};