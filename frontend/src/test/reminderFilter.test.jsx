import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { reminderService } from '../services/reminderService';
import { applicationService } from '../services/applicationService';
import api from '../services/api';
import { RemindersAndDeadlines } from '../pages/Reminders/RemindersPage';
import { ToastProvider } from '../context/ToastContext';

// Mock dependencies
vi.mock('../services/api');
vi.mock('../services/applicationService');
vi.mock('../services/reminderService');

const mockApplications = [
  { id: 1, companyName: 'Acme Corp', opportunityTitle: 'Frontend Engineer' },
  { id: 2, companyName: 'Beta Inc', opportunityTitle: 'Backend Engineer' },
];

const mockReminders = [
  {
    id: 101,
    title: 'Complete Hackerrank OA',
    reminderType: 'OA_DEADLINE',
    dueAt: '2026-10-05T18:00:00.000Z',
    completed: false,
    applicationId: 1,
  },
  {
    id: 102,
    title: 'Send Thank-you email',
    reminderType: 'FOLLOW_UP',
    dueAt: '2026-10-06T12:00:00.000Z',
    completed: true,
    applicationId: 2,
  },
];

describe('Reminder Filter & dueBefore Contract Suite', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    applicationService.getApplications.mockResolvedValue(mockApplications);
    reminderService.getReminders.mockResolvedValue(mockReminders);
  });

  describe('Service Layer Parameter Construction', () => {
    it('sends GET /reminders with no query parameters when params is empty', async () => {
      // Temporarily use unmocked reminderService implementation logic with mocked api
      api.get.mockResolvedValueOnce({
        data: { success: true, data: mockReminders, message: 'Reminders retrieved' },
      });

      // Call original service method using actual api.get
      const actualService = await vi.importActual('../services/reminderService');
      const data = await actualService.reminderService.getReminders({});

      expect(api.get).toHaveBeenCalledWith('/reminders', { params: {} });
      expect(data).toHaveLength(2);
    });

    it('sends correct parameter combinations without undefined or empty values', async () => {
      api.get.mockResolvedValueOnce({
        data: { success: true, data: [mockReminders[0]], message: 'Filtered' },
      });

      const actualService = await vi.importActual('../services/reminderService');
      const isoCutoff = new Date('2026-10-10T12:00:00Z').toISOString();
      const params = {
        completed: false,
        applicationId: 1,
        dueBefore: isoCutoff,
      };

      await actualService.reminderService.getReminders(params);

      expect(api.get).toHaveBeenCalledWith('/reminders', {
        params: {
          completed: false,
          applicationId: 1,
          dueBefore: isoCutoff,
        },
      });

      // Verify no extraneous or undefined keys
      const sentParams = api.get.mock.calls[0][1].params;
      expect(Object.keys(sentParams)).toEqual(['completed', 'applicationId', 'dueBefore']);
      expect(sentParams.dueBefore).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/);
    });
  });

  describe('RemindersPage Filter UI & Request Triggering', () => {
    it('fetches reminders with empty params on initial load when filters are ALL', async () => {
      render(
        <ToastProvider>
          <RemindersAndDeadlines />
        </ToastProvider>
      );

      await waitFor(() => {
        expect(reminderService.getReminders).toHaveBeenCalledTimes(1);
      });

      expect(reminderService.getReminders).toHaveBeenCalledWith({});
      expect(screen.getByText('Complete Hackerrank OA')).toBeInTheDocument();
    });

    it('sends completed=false when Active / Pending status is selected', async () => {
      render(
        <ToastProvider>
          <RemindersAndDeadlines />
        </ToastProvider>
      );

      await waitFor(() => {
        expect(reminderService.getReminders).toHaveBeenCalledTimes(1);
      });

      const statusSelect = screen.getByLabelText('Filter by Status');
      fireEvent.change(statusSelect, { target: { value: 'ACTIVE' } });

      await waitFor(() => {
        expect(reminderService.getReminders).toHaveBeenCalledTimes(2);
      });

      expect(reminderService.getReminders).toHaveBeenLastCalledWith({ completed: false });
    });

    it('sends completed=true when Completed status is selected', async () => {
      render(
        <ToastProvider>
          <RemindersAndDeadlines />
        </ToastProvider>
      );

      await waitFor(() => {
        expect(reminderService.getReminders).toHaveBeenCalledTimes(1);
      });

      const statusSelect = screen.getByLabelText('Filter by Status');
      fireEvent.change(statusSelect, { target: { value: 'COMPLETED' } });

      await waitFor(() => {
        expect(reminderService.getReminders).toHaveBeenCalledTimes(2);
      });

      expect(reminderService.getReminders).toHaveBeenLastCalledWith({ completed: true });
    });

    it('sends applicationId filter when application is selected', async () => {
      render(
        <ToastProvider>
          <RemindersAndDeadlines />
        </ToastProvider>
      );

      await waitFor(() => {
        expect(reminderService.getReminders).toHaveBeenCalledTimes(1);
      });

      const appSelect = screen.getByLabelText('Filter by Application');
      fireEvent.change(appSelect, { target: { value: '1' } });

      await waitFor(() => {
        expect(reminderService.getReminders).toHaveBeenCalledTimes(2);
      });

      expect(reminderService.getReminders).toHaveBeenLastCalledWith({ applicationId: '1' });
    });

    it('sends valid ISO Instant dueBefore cutoff when preset is chosen', async () => {
      render(
        <ToastProvider>
          <RemindersAndDeadlines />
        </ToastProvider>
      );

      await waitFor(() => {
        expect(reminderService.getReminders).toHaveBeenCalledTimes(1);
      });

      const dueSelect = screen.getByLabelText('Filter by Due Cutoff');
      fireEvent.change(dueSelect, { target: { value: '24H' } });

      await waitFor(() => {
        expect(reminderService.getReminders).toHaveBeenCalledTimes(2);
      });

      const lastCallParams = reminderService.getReminders.mock.calls[1][0];
      expect(lastCallParams.dueBefore).toBeDefined();
      expect(new Date(lastCallParams.dueBefore).toISOString()).toBe(lastCallParams.dueBefore);
    });

    it('sends combined filters (completed=false + applicationId + dueBefore)', async () => {
      render(
        <ToastProvider>
          <RemindersAndDeadlines />
        </ToastProvider>
      );

      await waitFor(() => {
        expect(reminderService.getReminders).toHaveBeenCalledTimes(1);
      });

      fireEvent.change(screen.getByLabelText('Filter by Status'), { target: { value: 'ACTIVE' } });
      fireEvent.change(screen.getByLabelText('Filter by Application'), { target: { value: '2' } });
      fireEvent.change(screen.getByLabelText('Filter by Due Cutoff'), { target: { value: '48H' } });

      await waitFor(() => {
        const lastCallParams = reminderService.getReminders.mock.calls[reminderService.getReminders.mock.calls.length - 1][0];
        expect(lastCallParams.completed).toBe(false);
        expect(lastCallParams.applicationId).toBe('2');
        expect(lastCallParams.dueBefore).toBeDefined();
      });
    });

    it('does not send dueBefore parameter when dueBeforePreset is ALL', async () => {
      render(
        <ToastProvider>
          <RemindersAndDeadlines />
        </ToastProvider>
      );

      await waitFor(() => {
        expect(reminderService.getReminders).toHaveBeenCalledTimes(1);
      });

      const lastCallParams = reminderService.getReminders.mock.calls[0][0];
      expect(lastCallParams.dueBefore).toBeUndefined();
    });
  });
});
