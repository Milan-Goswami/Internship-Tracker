import React, { useState, useEffect, useCallback } from 'react';
import { Skeleton } from '../../components/common/Skeleton';
import { ErrorState } from '../../components/states/ErrorState';
import { useToast } from '../../context/ToastContext';
import { userService } from '../../services/userService';

import { AccountInfoCard } from './components/AccountInfoCard';
import { PreferencesForm } from './components/PreferencesForm';

import './SettingsPage.css';

export const CandidatePreferencesAndSettings = () => {
  const { showToast } = useToast();

  const [profile, setProfile] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState(null);

  const fetchProfile = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await userService.getUserProfile();
      setProfile(data);
    } catch (err) {
      console.error('Profile fetch error:', err);
      setError(err.response?.data?.message || err.message || 'Failed to load profile settings');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchProfile();
  }, [fetchProfile]);

  const handleSavePreferences = async (payload) => {
    setIsSaving(true);
    try {
      const updated = await userService.updatePreferences(payload);
      setProfile(updated);
      showToast('Candidate preferences saved successfully', 'success');
    } catch (err) {
      console.error('Update preferences error:', err);
      showToast(err.response?.data?.message || err.message || 'Failed to update preferences', 'error');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="settings-page">
      <header className="settings-page-header">
        <div>
          <h1 className="settings-page-title">Candidate Preferences & Settings</h1>
          <p className="settings-page-subtitle">
            Configure your target internship specifications, geographic markets, and account parameters.
          </p>
        </div>
      </header>

      {isLoading ? (
        <div className="settings-loading-container">
          <Skeleton width="100%" height="160px" style={{ borderRadius: '12px', marginBottom: 'var(--space-6)' }} />
          <Skeleton width="100%" height="380px" style={{ borderRadius: '12px' }} />
        </div>
      ) : error ? (
        <ErrorState
          title="Failed to Load Settings"
          description={error}
          onRetry={fetchProfile}
        />
      ) : (
        <div className="settings-content">
          {/* Account Credentials / Identity Card */}
          <AccountInfoCard userProfile={profile} />

          {/* Target Role & Search Preferences Form */}
          <PreferencesForm
            initialProfile={profile}
            onSave={handleSavePreferences}
            isSaving={isSaving}
          />
        </div>
      )}
    </div>
  );
};

export const SettingsPage = CandidatePreferencesAndSettings;
export default CandidatePreferencesAndSettings;
