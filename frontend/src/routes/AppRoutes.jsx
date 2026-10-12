import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute } from './ProtectedRoute';
import { PublicOnlyRoute } from './PublicOnlyRoute';
import { AppLayout } from '../layouts/AppLayout';
import { AuthLayout } from '../layouts/AuthLayout';
import { LoginPage } from '../pages/Auth/LoginPage';
import { RegisterPage } from '../pages/Auth/RegisterPage';

import { CommandCenterDashboard as DashboardPage } from '../pages/Dashboard/DashboardPage';
import { ApplicationsWorkspace as ApplicationsPage } from '../pages/Applications/ApplicationsPage';
import { OpportunitiesCatalog as OpportunitiesPage } from '../pages/Opportunities/OpportunitiesPage';
import { InterviewHub as InterviewsPage } from '../pages/Interviews/InterviewsPage';
import { RemindersAndDeadlines as RemindersPage } from '../pages/Reminders/RemindersPage';
import { SkillMatrixAndTaxonomy as SkillMatrixPage } from '../pages/Skills/SkillMatrixPage';
import { DocumentsLibrary as DocumentsPage } from '../pages/Documents/DocumentsPage';
import { FunnelAnalyticsAndIntelligence as AnalyticsPage } from '../pages/Analytics/AnalyticsPage';
import { CandidatePreferencesAndSettings as SettingsPage } from '../pages/Settings/SettingsPage';

export const AppRoutes = () => {
  return (
    <Routes>
      {/* Public Auth Routes */}
      <Route
        element={
          <PublicOnlyRoute>
            <AuthLayout />
          </PublicOnlyRoute>
        }
      >
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>

      {/* Protected App Routes */}
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="applications" element={<ApplicationsPage />} />
        <Route path="opportunities" element={<OpportunitiesPage />} />
        <Route path="interviews" element={<InterviewsPage />} />
        <Route path="reminders" element={<RemindersPage />} />
        <Route path="skills" element={<SkillMatrixPage />} />
        <Route path="documents" element={<DocumentsPage />} />
        <Route path="analytics" element={<AnalyticsPage />} />
        <Route path="settings" element={<SettingsPage />} />
      </Route>

      {/* Catch-all fallback */}
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
};