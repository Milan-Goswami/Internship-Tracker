import React from 'react';
import './Button.css';

export const Button = ({
  children,
  variant = 'primary',
  size = 'md',
  isLoading = false,
  loading = false,
  disabled = false,
  className = '',
  icon = null,
  ...props
}) => {
  const isBusy = isLoading || loading;
  const classes = ['btn', `btn-${variant}`, `btn-${size}`, className].filter(Boolean).join(' ');

  return (
    <button
      className={classes}
      disabled={disabled || isBusy}
      {...props}
    >
      {isBusy ? (
        <span className="btn-spinner" />
      ) : (
        icon && <span className="btn-icon">{icon}</span>
      )}
      <span>{children}</span>
    </button>
  );
};