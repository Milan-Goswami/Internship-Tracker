import React from 'react';
import { Button } from '../common/Button';
import './EmptyState.css';

export const EmptyState = ({
  icon,
  title,
  description,
  actionLabel,
  onAction,
  headingLevel = 'h2',
}) => {
  const HeadingTag = headingLevel;
  return (
    <div className="empty-state">
      {icon && <div className="empty-state-icon">{icon}</div>}
      <HeadingTag className="empty-state-title">{title}</HeadingTag>
      <p className="empty-state-desc">{description}</p>
      {actionLabel && onAction && (
        <Button variant="primary" onClick={onAction}>
          {actionLabel}
        </Button>
      )}
    </div>
  );
};