import React from 'react';
import './Badge.css';

export const Badge = ({ children, status = 'default', className = '' }) => {
  const normalizedStatus = (status || 'default').toLowerCase();
  const classes = ['badge', `badge-${normalizedStatus}`, className].filter(Boolean).join(' ');

  return (
    <span className={classes}>
      {children}
    </span>
  );
};