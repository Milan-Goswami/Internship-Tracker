import React from 'react';
import './Input.css';

export const Input = ({
  label,
  error,
  id,
  type = 'text',
  className = '',
  ...props
}) => {
  const containerClasses = ['input-container', error ? 'input-error' : ''].filter(Boolean).join(' ');
  const wrapperClasses = ['input-wrapper', className].filter(Boolean).join(' ');

  return (
    <div className={wrapperClasses}>
      {label && (
        <label htmlFor={id} className="input-label">
          {label}
        </label>
      )}
      <div className={containerClasses}>
        <input
          id={id}
          type={type}
          className="input-field"
          {...props}
        />
      </div>
      {error && <span className="input-error-msg">{error}</span>}
    </div>
  );
};