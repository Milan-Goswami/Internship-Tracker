import React, { useState, useEffect } from 'react';
import { Button } from '../../../components/common/Button';
import '../SettingsPage.css';

const WORK_MODE_OPTIONS = [
  { value: 'ANY', label: 'Any Work Mode (Remote / Hybrid / Onsite)' },
  { value: 'REMOTE', label: 'Remote Only' },
  { value: 'HYBRID', label: 'Hybrid' },
  { value: 'ONSITE', label: 'Onsite Only' },
];

export const PreferencesForm = ({ initialProfile, onSave, isSaving }) => {
  const [fullName, setFullName] = useState('');
  const [targetTitle, setTargetTitle] = useState('');
  const [targetLocation, setTargetLocation] = useState('');
  const [preferredWorkMode, setPreferredWorkMode] = useState('ANY');
  const [targetSalaryMin, setTargetSalaryMin] = useState('');
  const [targetSalaryMax, setTargetSalaryMax] = useState('');

  const [validationError, setValidationError] = useState(null);

  useEffect(() => {
    if (initialProfile) {
      setFullName(initialProfile.fullName || '');
      setTargetTitle(initialProfile.targetTitle || '');
      setTargetLocation(initialProfile.targetLocation || '');
      setPreferredWorkMode(initialProfile.preferredWorkMode || 'ANY');
      setTargetSalaryMin(
        initialProfile.targetSalaryMin !== null && initialProfile.targetSalaryMin !== undefined
          ? String(initialProfile.targetSalaryMin)
          : ''
      );
      setTargetSalaryMax(
        initialProfile.targetSalaryMax !== null && initialProfile.targetSalaryMax !== undefined
          ? String(initialProfile.targetSalaryMax)
          : ''
      );
    }
  }, [initialProfile]);

  const handleSubmit = (e) => {
    e.preventDefault();
    setValidationError(null);

    const minNum = targetSalaryMin !== '' ? Number(targetSalaryMin) : null;
    const maxNum = targetSalaryMax !== '' ? Number(targetSalaryMax) : null;

    if (minNum !== null && minNum < 0) {
      setValidationError('Target minimum salary cannot be negative.');
      return;
    }

    if (maxNum !== null && maxNum < 0) {
      setValidationError('Target maximum salary cannot be negative.');
      return;
    }

    if (minNum !== null && maxNum !== null && minNum > maxNum) {
      setValidationError('Target minimum salary cannot exceed target maximum salary.');
      return;
    }

    const payload = {
      fullName: fullName.trim() || null,
      targetTitle: targetTitle.trim() || null,
      targetLocation: targetLocation.trim() || null,
      preferredWorkMode,
      targetSalaryMin: minNum,
      targetSalaryMax: maxNum,
    };

    onSave(payload);
  };

  return (
    <div className="settings-card">
      <div className="settings-card-header">
        <div className="settings-card-header-icon" style={{ color: '#10b981', backgroundColor: 'rgba(16, 185, 129, 0.1)' }}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="12" cy="12" r="3" />
            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
          </svg>
        </div>
        <div>
          <h2 className="settings-card-title">Search & Target Criteria</h2>
          <p className="settings-card-subtitle">
            Preferred internship role specs, target geographic markets, and compensation baseline.
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="preferences-form">
        {validationError && (
          <div className="settings-error-banner" role="alert">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            <span>{validationError}</span>
          </div>
        )}

        <div className="settings-form-row">
          <div className="settings-field-group">
            <label className="settings-label" htmlFor="fullName">Candidate Full Name</label>
            <input
              id="fullName"
              type="text"
              className="settings-input"
              placeholder="e.g. Alex Chen"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              maxLength={100}
            />
            <span className="settings-field-hint">Displayed across your dashboard and profile.</span>
          </div>

          <div className="settings-field-group">
            <label className="settings-label" htmlFor="targetTitle">Target Internship Title</label>
            <input
              id="targetTitle"
              type="text"
              className="settings-input"
              placeholder="e.g. Software Engineering Intern"
              value={targetTitle}
              onChange={(e) => setTargetTitle(e.target.value)}
              maxLength={100}
            />
            <span className="settings-field-hint">Primary role title used for matching recommendations.</span>
          </div>
        </div>

        <div className="settings-form-row">
          <div className="settings-field-group">
            <label className="settings-label" htmlFor="targetLocation">Target Geographic Location</label>
            <input
              id="targetLocation"
              type="text"
              className="settings-input"
              placeholder="e.g. San Francisco, CA or Remote"
              value={targetLocation}
              onChange={(e) => setTargetLocation(e.target.value)}
              maxLength={100}
            />
            <span className="settings-field-hint">Preferred cities or regions for onsite/hybrid roles.</span>
          </div>

          <div className="settings-field-group">
            <label className="settings-label" htmlFor="preferredWorkMode">Preferred Work Mode</label>
            <select
              id="preferredWorkMode"
              className="settings-select"
              value={preferredWorkMode}
              onChange={(e) => setPreferredWorkMode(e.target.value)}
            >
              {WORK_MODE_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
            <span className="settings-field-hint">Filters opportunity catalog recommendations.</span>
          </div>
        </div>

        <div className="settings-form-row">
          <div className="settings-field-group">
            <label className="settings-label" htmlFor="targetSalaryMin">Target Hourly/Monthly Min Salary ($)</label>
            <input
              id="targetSalaryMin"
              type="number"
              step="1"
              className="settings-input"
              placeholder="e.g. 45 (or 7500/mo)"
              value={targetSalaryMin}
              onChange={(e) => setTargetSalaryMin(e.target.value)}
            />
            <span className="settings-field-hint">Minimum acceptable baseline compensation.</span>
          </div>

          <div className="settings-field-group">
            <label className="settings-label" htmlFor="targetSalaryMax">Target Hourly/Monthly Max Salary ($)</label>
            <input
              id="targetSalaryMax"
              type="number"
              step="1"
              className="settings-input"
              placeholder="e.g. 75 (or 12000/mo)"
              value={targetSalaryMax}
              onChange={(e) => setTargetSalaryMax(e.target.value)}
            />
            <span className="settings-field-hint">Target ceiling or dream compensation.</span>
          </div>
        </div>

        <div className="settings-form-footer">
          <Button
            type="submit"
            variant="primary"
            isLoading={isSaving}
          >
            Save Preferences
          </Button>
        </div>
      </form>
    </div>
  );
};
