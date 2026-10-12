import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { reminderService } from '../services/reminderService';
import { applicationService } from '../services/applicationService';
import { RemindersAndDeadlines } from '../pages/Reminders/RemindersPage';
import { ToastProvider } from '../context/ToastContext';

vi.mock('../services/applicationService');
vi.mock('../services/reminderService');

const initialReminders = [
  {
    id: 201,
    title: 'Prepare System Design Notes',
    reminderType: 'INTERVIEW_PREP',
    dueAt: '2026-10-08T15:00:00.000Z',
    completed: false,
    applicationId: 1,
  },
];

describe('Reminder Optimistic Update & Rollback Suite', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    applicationService.getApplications.mockResolvedValue([
      { id: 1, companyName: 'Stripe', opportunityTitle: 'Software Engineer' },
    ]);
    reminderService.getReminders.mockResolvedValue([...initialReminders]);
  });

  it('optimistically marks reminder as completed immediately, then reconciles on success', async () => {
    // Delay resolution to capture intermediate optimistic DOM state
    let resolveComplete;
    reminderService.completeReminder.mockReturnValue(
      new Promise((resolve) => {
        resolveComplete = resolve;
      })
    );

    render(
      <ToastProvider>
        <RemindersAndDeadlines />
      </ToastProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('Prepare System Design Notes')).toBeInTheDocument();
    });

    const checkbox = screen.getByRole('checkbox', { name: /mark .* as complete/i });
    expect(checkbox).not.toBeChecked();

    // Trigger toggle complete
    fireEvent.click(checkbox);

    // 1. Assert optimistic update immediately in the DOM
    expect(checkbox).toBeChecked();

    // 2. Resolve backend API successfully
    resolveComplete({
      id: 201,
      title: 'Prepare System Design Notes',
      reminderType: 'INTERVIEW_PREP',
      dueAt: '2026-10-08T15:00:00.000Z',
      completed: true,
      applicationId: 1,
    });

    await waitFor(() => {
      expect(reminderService.completeReminder).toHaveBeenCalledWith(201);
    });

    // Checkbox remains completed in confirmed state
    const completedCheckbox = screen.getByRole('checkbox', { name: /as incomplete/i });
    expect(completedCheckbox).toBeChecked();
    expect(screen.getByText('Reminder marked as completed')).toBeInTheDocument();
  });

  it('rolls back to previous state and displays error notification when API fails', async () => {
    reminderService.completeReminder.mockRejectedValueOnce({
      response: {
        data: {
          success: false,
          message: 'Failed to update reminder on server',
        },
      },
    });

    render(
      <ToastProvider>
        <RemindersAndDeadlines />
      </ToastProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('Prepare System Design Notes')).toBeInTheDocument();
    });

    const checkbox = screen.getByRole('checkbox', { name: /as complete/i });
    expect(checkbox).not.toBeChecked();

    // Trigger toggle complete
    fireEvent.click(checkbox);

    // Assert API called
    await waitFor(() => {
      expect(reminderService.completeReminder).toHaveBeenCalledWith(201);
    });

    // Rollback must have restored state to false (unchecked)
    await waitFor(() => {
      const rolledBackCheckbox = screen.getByRole('checkbox', { name: /as complete/i });
      expect(rolledBackCheckbox).not.toBeChecked();
    });

    // Error notification must be rendered
    expect(screen.getByText('Failed to update reminder on server')).toBeInTheDocument();
  });
});
