import React from 'react';
import '../SettingsPage.css';

export const AccountInfoCard = ({ userProfile }) => {
  const formatDate = (isoString) => {
    if (!isoString) return 'Active Member';
    const d = new Date(isoString);
    return d.toLocaleDateString(undefined, { month: 'long', day: 'numeric', year: 'numeric' });
  };

  return (
    <div className="settings-card">
      <div className="settings-card-header">
        <div className="settings-card-header-icon" style={{ color: '#3b82f6', backgroundColor: 'rgba(59, 130, 246, 0.1)' }}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
            <circle cx="12" cy="7" r="4" />
          </svg>
        </div>
        <div>
          <h2 className="settings-card-title">Account Identity</h2>
          <p className="settings-card-subtitle">
            Authenticated credentials and profile identifier.
          </p>
        </div>
      </div>

      <div className="account-details-grid">
        <div className="account-field">
          <span className="account-field-label">Email Address</span>
          <div className="account-field-value-box">
            <span className="account-email-value">{userProfile?.email}</span>
            <span className="account-verified-badge">Verified</span>
          </div>
        </div>

        <div className="account-field">
          <span className="account-field-label">Account Role</span>
          <div className="account-field-value-box">
            <span className="account-role-pill">
              {userProfile?.role === 'ROLE_USER' ? 'CANDIDATE' : (userProfile?.role || 'CANDIDATE')}
            </span>
          </div>
        </div>

        <div className="account-field">
          <span className="account-field-label">Account Status</span>
          <div className="account-field-value-box">
            <span className="account-status-indicator">
              <span className="status-ping-dot" />
              Active
            </span>
          </div>
        </div>

        <div className="account-field">
          <span className="account-field-label">Member Since</span>
          <div className="account-field-value-box">
            <span className="account-created-date">{formatDate(userProfile?.createdAt)}</span>
          </div>
        </div>
      </div>
    </div>
  );
};
