import React from 'react';
import { KanbanCard } from './KanbanCard';
import './Kanban.css';

const STAGE_COLORS = {
  SAVED: '#64748B',
  APPLIED: '#3B82F6',
  SCREENING: '#8B5CF6',
  INTERVIEWING: '#F59E0B',
  OFFER: '#10B981',
  ACCEPTED: '#10B981',
  REJECTED: '#EF4444',
  WITHDRAWN: '#71717A',
};

export const KanbanColumn = ({
  stage,
  title,
  applications,
  onStatusChange,
  onEdit,
  onDelete,
}) => {
  const color = STAGE_COLORS[stage] || '#64748B';

  return (
    <div className="kanban-column">
      <div className="kanban-column-header">
        <div className="column-header-title">
          <span className="column-color-indicator" style={{ backgroundColor: color }} />
          <span>{title}</span>
        </div>
        <span className="column-card-count">{applications.length}</span>
      </div>

      <div className="kanban-column-cards">
        {applications.length === 0 ? (
          <div className="kanban-empty-column">No applications in this stage</div>
        ) : (
          applications.map((app) => (
            <KanbanCard
              key={app.id}
              application={app}
              onStatusChange={onStatusChange}
              onEdit={onEdit}
              onDelete={onDelete}
            />
          ))
        )}
      </div>
    </div>
  );
};