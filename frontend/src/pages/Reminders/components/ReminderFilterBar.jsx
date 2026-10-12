import React from 'react';
import { Button } from '../../../components/common/Button';
import './ReminderFilterBar.css';

export const ReminderFilterBar = ({
  searchQuery,
  onSearchChange,
  statusFilter,
  onStatusFilterChange,
  applicationFilter,
  onApplicationFilterChange,
  dueBeforePreset = 'ALL',
  onDueBeforePresetChange,
  applications = [],
  onCreateClick,
}) => {
  return (
    <div className="reminder-filter-bar">
      <div className="reminder-search-wrapper">
        <svg
          className="reminder-search-icon"
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
          className="reminder-search-input"
          placeholder="Search reminders by task title..."
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
          aria-label="Search reminders"
        />
      </div>

      <div className="reminder-filter-controls">
        <select
          className="reminder-select"
          value={statusFilter}
          onChange={(e) => onStatusFilterChange(e.target.value)}
          aria-label="Filter by Status"
        >
          <option value="ALL">All Statuses</option>
          <option value="ACTIVE">Active / Pending</option>
          <option value="COMPLETED">Completed</option>
        </select>

        <select
          className="reminder-select"
          value={dueBeforePreset}
          onChange={(e) => onDueBeforePresetChange && onDueBeforePresetChange(e.target.value)}
          aria-label="Filter by Due Cutoff"
        >
          <option value="ALL">Due: Anytime</option>
          <option value="TODAY">Due: Today</option>
          <option value="24H">Due: Within 24h</option>
          <option value="48H">Due: Within 48h</option>
          <option value="7D">Due: Within 7 Days</option>
        </select>

        <select
          className="reminder-select"
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

        <Button variant="primary" size="md" onClick={onCreateClick}>
          + Create Reminder
        </Button>
      </div>
    </div>
  );
};
