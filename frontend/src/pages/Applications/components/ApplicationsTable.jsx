import React from 'react';
import './ApplicationsTable.css';

const STAGES = [
  'SAVED',
  'APPLIED',
  'SCREENING',
  'INTERVIEWING',
  'OFFER',
  'ACCEPTED',
  'REJECTED',
  'WITHDRAWN',
];

export const ApplicationsTable = ({
  applications,
  selectedIds,
  onToggleSelect,
  onToggleSelectAll,
  sortField,
  sortOrder,
  onSort,
  onStatusChange,
  onEdit,
  onDelete,
}) => {
  const selectAllRef = React.useRef(null);
  const [activeIndex, setActiveIndex] = React.useState(0);
  const rowRefs = React.useRef([]);

  const someSelected = applications.some((app) => selectedIds.includes(app.id));
  const isAllSelected =
    applications.length > 0 && applications.every((app) => selectedIds.includes(app.id));
  const isIndeterminate = someSelected && !isAllSelected;

  React.useEffect(() => {
    if (selectAllRef.current) {
      selectAllRef.current.indeterminate = isIndeterminate;
    }
  }, [isIndeterminate]);

  React.useEffect(() => {
    if (applications.length === 0) {
      setActiveIndex(0);
    } else if (activeIndex >= applications.length) {
      setActiveIndex(applications.length - 1);
    }
  }, [applications.length, activeIndex]);

  const handleRowKeyDown = (e, index) => {
    const targetTag = e.target.tagName;
    const isInteractiveDescendant = ['BUTTON', 'INPUT', 'SELECT', 'A'].includes(targetTag);

    if (e.key === 'ArrowDown' || e.key === 'j' || e.key === 'J') {
      if (targetTag === 'SELECT' || targetTag === 'INPUT') return;
      e.preventDefault();
      const nextIndex = Math.min(index + 1, applications.length - 1);
      setActiveIndex(nextIndex);
      rowRefs.current[nextIndex]?.focus();
    } else if (e.key === 'ArrowUp' || e.key === 'k' || e.key === 'K') {
      if (targetTag === 'SELECT' || targetTag === 'INPUT') return;
      e.preventDefault();
      const prevIndex = Math.max(index - 1, 0);
      setActiveIndex(prevIndex);
      rowRefs.current[prevIndex]?.focus();
    } else if (e.key === 'Enter') {
      if (isInteractiveDescendant) return;
      e.preventDefault();
      if (applications[index]) {
        onEdit(applications[index]);
      }
    } else if (e.key === ' ' || e.key === 'Spacebar') {
      if (isInteractiveDescendant) return;
      e.preventDefault();
      if (applications[index]) {
        onToggleSelect(applications[index].id);
      }
    }
  };

  const renderSortIndicator = (field) => {
    if (sortField !== field) {
      return <span className="sort-icon">↕</span>;
    }
    return <span className="sort-icon active">{sortOrder === 'asc' ? '▲' : '▼'}</span>;
  };

  return (
    <div className="applications-table-wrapper">
      <table className="applications-table" role="table" aria-label="Applications list">
        <thead>
          <tr>
            <th style={{ width: '40px' }}>
              <input
                ref={selectAllRef}
                type="checkbox"
                className="table-checkbox"
                checked={isAllSelected}
                onChange={onToggleSelectAll}
                aria-checked={isIndeterminate ? 'mixed' : isAllSelected}
                aria-label={isAllSelected ? 'Deselect all applications' : 'Select all applications'}
              />
            </th>
            <th className="sortable" onClick={() => onSort('companyName')}>
              <div className="th-sort-wrapper">
                <span>Company</span>
                {renderSortIndicator('companyName')}
              </div>
            </th>
            <th className="sortable" onClick={() => onSort('opportunityTitle')}>
              <div className="th-sort-wrapper">
                <span>Role Title</span>
                {renderSortIndicator('opportunityTitle')}
              </div>
            </th>
            <th className="sortable" onClick={() => onSort('status')}>
              <div className="th-sort-wrapper">
                <span>Stage</span>
                {renderSortIndicator('status')}
              </div>
            </th>
            <th className="sortable" onClick={() => onSort('userRating')}>
              <div className="th-sort-wrapper">
                <span>Rating</span>
                {renderSortIndicator('userRating')}
              </div>
            </th>
            <th className="sortable" onClick={() => onSort('date')}>
              <div className="th-sort-wrapper">
                <span>Date</span>
                {renderSortIndicator('date')}
              </div>
            </th>
            <th>Notes</th>
            <th style={{ textAlign: 'right' }}>Actions</th>
          </tr>
        </thead>
        <tbody>
          {applications.map((app, index) => {
            const isSelected = selectedIds.includes(app.id);
            const isActive = index === activeIndex;
            const formattedDate = app.appliedAt
              ? new Date(app.appliedAt).toLocaleDateString(undefined, {
                  month: 'short',
                  day: 'numeric',
                  year: 'numeric',
                })
              : app.savedAt
              ? new Date(app.savedAt).toLocaleDateString(undefined, {
                  month: 'short',
                  day: 'numeric',
                  year: 'numeric',
                })
              : '—';

            const initial = app.companyName ? app.companyName.charAt(0).toUpperCase() : 'O';

            return (
              <tr
                key={app.id}
                ref={(el) => (rowRefs.current[index] = el)}
                tabIndex={isActive ? 0 : -1}
                role="row"
                aria-selected={isSelected}
                aria-current={isActive ? 'true' : undefined}
                className={[
                  isSelected ? 'selected' : '',
                  isActive ? 'active-row' : '',
                ]
                  .filter(Boolean)
                  .join(' ')}
                onClick={(e) => {
                  if (!['BUTTON', 'INPUT', 'SELECT', 'A'].includes(e.target.tagName)) {
                    setActiveIndex(index);
                    rowRefs.current[index]?.focus();
                  }
                }}
                onFocus={(e) => {
                  if (!['BUTTON', 'INPUT', 'SELECT', 'A'].includes(e.target.tagName)) {
                    setActiveIndex(index);
                  }
                }}
                onKeyDown={(e) => handleRowKeyDown(e, index)}
              >
                <td>
                  <input
                    type="checkbox"
                    className="table-checkbox"
                    checked={isSelected}
                    onChange={() => onToggleSelect(app.id)}
                    aria-label={`Select ${app.companyName} - ${app.opportunityTitle}`}
                  />
                </td>
                <td>
                  <div className="table-company-cell">
                    <div className="table-company-avatar">{initial}</div>
                    <span className="table-company-name">{app.companyName || 'Unknown Company'}</span>
                  </div>
                </td>
                <td>
                  <span className="table-role-title">
                    {app.opportunityTitle || 'Software Engineering Intern'}
                  </span>
                </td>
                <td>
                  <select
                    className="table-status-select"
                    value={app.status}
                    onChange={(e) => onStatusChange(app.id, e.target.value)}
                    aria-label={`Status for ${app.companyName || 'application'}`}
                  >
                    {STAGES.map((s) => (
                      <option key={s} value={s}>
                        {s}
                      </option>
                    ))}
                  </select>
                </td>
                <td>
                  {app.userRating ? (
                    <div style={{ display: 'flex', gap: '2px', color: '#F59E0B', fontSize: '11px' }}>
                      {[1, 2, 3, 4, 5].map((star) => (
                        <span key={star} style={{ opacity: star <= app.userRating ? 1 : 0.2 }}>
                          ★
                        </span>
                      ))}
                    </div>
                  ) : (
                    <span style={{ color: 'var(--color-text-muted)', fontSize: '11px' }}>—</span>
                  )}
                </td>
                <td style={{ color: 'var(--color-text-secondary)', fontSize: '12px' }}>{formattedDate}</td>
                <td>
                  <div className="table-notes-preview" title={app.personalNotes || ''}>
                    {app.personalNotes || '—'}
                  </div>
                </td>
                <td>
                  <div className="table-actions-cell" style={{ justifyContent: 'flex-end' }}>
                    <button
                      className="table-action-icon-btn"
                      onClick={() => onEdit(app)}
                      title="Edit application"
                      aria-label={`Edit application for ${app.companyName || 'application'}`}
                    >
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                      </svg>
                    </button>
                    <button
                      className="table-action-icon-btn delete"
                      onClick={() => onDelete(app)}
                      title="Delete application"
                      aria-label={`Delete application for ${app.companyName || 'application'}`}
                    >
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <polyline points="3 6 5 6 21 6" />
                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};