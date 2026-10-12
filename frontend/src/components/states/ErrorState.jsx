import React from 'react';
import { Button } from '../common/Button';
import './ErrorState.css';

export const ErrorState = ({
  title = 'Unable to Load Data',
  description = 'An unexpected connection error occurred. Please try again.',
  onRetry,
  headingLevel = 'h2',
}) => {
  const HeadingTag = headingLevel === 'h3' ? 'h3' : headingLevel === 'h4' ? 'h4' : 'h2';
  return (
    <div className="error-state">
      <HeadingTag className="error-state-title">{title}</HeadingTag>
      <p className="error-state-desc">{description}</p>
      {onRetry && (
        <Button variant="secondary" size="sm" onClick={onRetry}>
          Retry
        </Button>
      )}
    </div>
  );
};