import React, { useState } from 'react';
import { KanbanColumn } from './KanbanColumn';
import './Kanban.css';

const PIPELINE_STAGES = [
  { key: 'SAVED', title: 'Saved' },
  { key: 'APPLIED', title: 'Applied' },
  { key: 'SCREENING', title: 'Screening' },
  { key: 'INTERVIEWING', title: 'Interviewing' },
  { key: 'OFFER', title: 'Offer' },
];

const TERMINAL_STAGES = [
  { key: 'ACCEPTED', title: 'Accepted' },
  { key: 'REJECTED', title: 'Rejected' },
  { key: 'WITHDRAWN', title: 'Withdrawn' },
];

export const KanbanBoard = ({
  applications,
  onStatusChange,
  onEdit,
  onDelete,
}) => {
  const [showTerminal, setShowTerminal] = useState(false);

  const getAppsForStage = (stage) => {
    return applications.filter((app) => app.status === stage);
  };

  const terminalCount = applications.filter((app) =>
    ['ACCEPTED', 'REJECTED', 'WITHDRAWN'].includes(app.status)
  ).length;

  return (
    <div className="kanban-board-container">
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 'var(--space-2)' }}>
        <button
          className="clear-filters-btn"
          onClick={() => setShowTerminal(!showTerminal)}
          style={{ fontSize: 'var(--font-size-xs)', textDecoration: 'none', color: 'var(--color-text-secondary)' }}
        >
          {showTerminal ? 'Hide Closed Columns ▲' : `Show Closed Columns (${terminalCount}) ▼`}
        </button>
      </div>

      <div className="kanban-board">
        {PIPELINE_STAGES.map(({ key, title }) => (
          <KanbanColumn
            key={key}
            stage={key}
            title={title}
            applications={getAppsForStage(key)}
            onStatusChange={onStatusChange}
            onEdit={onEdit}
            onDelete={onDelete}
          />
        ))}

        {showTerminal &&
          TERMINAL_STAGES.map(({ key, title }) => (
            <KanbanColumn
              key={key}
              stage={key}
              title={title}
              applications={getAppsForStage(key)}
              onStatusChange={onStatusChange}
              onEdit={onEdit}
              onDelete={onDelete}
            />
          ))}
      </div>
    </div>
  );
};