import React from 'react';
import { Button } from '../../../components/common/Button';
import './OpportunityFilterBar.css';

export const OpportunityFilterBar = ({
  searchQuery,
  onSearchChange,
  locationFilter,
  onLocationFilterChange,
  sourceFilter,
  onSourceFilterChange,
  matchFilter,
  onMatchFilterChange,
  onClearFilters,
  onAddClick,
  filteredCount,
  totalCount,
}) => {
  const isFiltered =
    Boolean(searchQuery.trim()) ||
    locationFilter !== 'ALL' ||
    sourceFilter !== 'ALL' ||
    matchFilter !== 'ALL';

  return (
    <div className="opportunity-filter-bar">
      <div className="opportunity-filter-top">
        <div className="opportunity-search-wrapper">
          <span className="opportunity-search-icon">🔍</span>
          <input
            type="text"
            className="opportunity-search-input"
            placeholder="Search by role title, company, or tech keywords..."
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
            aria-label="Search opportunities"
          />
          {searchQuery && (
            <button
              className="opportunity-search-clear"
              onClick={() => onSearchChange('')}
              aria-label="Clear search query"
            >
              ✕
            </button>
          )}
        </div>

        <Button variant="primary" size="md" onClick={onAddClick}>
          + Add Custom Opportunity
        </Button>
      </div>

      <div className="opportunity-filter-bottom">
        <div className="opportunity-filter-groups">
          <select
            className="opportunity-select"
            value={locationFilter}
            onChange={(e) => onLocationFilterChange(e.target.value)}
            aria-label="Filter by Location Mode"
          >
            <option value="ALL">All Work Modes</option>
            <option value="REMOTE">Remote</option>
            <option value="HYBRID">Hybrid</option>
            <option value="ONSITE">On-site</option>
          </select>

          <select
            className="opportunity-select"
            value={sourceFilter}
            onChange={(e) => onSourceFilterChange(e.target.value)}
            aria-label="Filter by Opportunity Source"
          >
            <option value="ALL">All Sources</option>
            <option value="GLOBAL">Global Postings</option>
            <option value="CUSTOM">Custom Opportunities</option>
          </select>

          <select
            className="opportunity-select"
            value={matchFilter}
            onChange={(e) => onMatchFilterChange(e.target.value)}
            aria-label="Filter by Skill Match"
          >
            <option value="ALL">All Match Levels</option>
            <option value="HIGH">High Match (≥ 70%)</option>
            <option value="MATCHED">Any Match (&gt; 0%)</option>
          </select>

          {isFiltered && (
            <button className="opportunity-clear-btn" onClick={onClearFilters}>
              Reset Filters
            </button>
          )}
        </div>

        <div className="opportunity-filter-meta">
          <span className="opportunity-count">
            Showing {filteredCount} of {totalCount} {totalCount === 1 ? 'opportunity' : 'opportunities'}
          </span>
        </div>
      </div>
    </div>
  );
};
