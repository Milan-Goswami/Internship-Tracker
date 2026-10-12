import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { applicationService } from '../services/applicationService';
import { ApplicationsWorkspace } from '../pages/Applications/ApplicationsPage';
import { ToastProvider } from '../context/ToastContext';

// Mock dependencies
vi.mock('../services/applicationService');
vi.mock('../services/api');

const mockApplications = [
  { id: 1, companyName: 'Acme Corp', opportunityTitle: 'Frontend Engineer', status: 'APPLIED' },
  { id: 2, companyName: 'Beta Inc', opportunityTitle: 'Backend Engineer', status: 'SAVED' },
  { id: 3, companyName: 'Gamma LLC', opportunityTitle: 'Data Scientist', status: 'SCREENING' }
];

describe('Applications Batch Update & Row Selection Suite', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    applicationService.getApplications.mockResolvedValue(mockApplications);
    
    // Default online
    vi.stubGlobal('navigator', { onLine: true });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  const renderComponent = () => {
    render(
      <ToastProvider>
        <ApplicationsWorkspace />
      </ToastProvider>
    );
  };

  it('renders applications and allows switching to table mode', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Acme Corp')).toBeInTheDocument();
    });

    // Switch to table mode
    const tableModeButton = screen.getByRole('button', { name: /table/i });
    fireEvent.click(tableModeButton);

    await waitFor(() => {
      expect(screen.getByRole('table')).toBeInTheDocument();
    });
  });

  it('handles row selection counts and batch status API calls', async () => {
    applicationService.batchUpdateStatus.mockResolvedValue([
      { id: 1, status: 'INTERVIEWING' },
      { id: 2, status: 'INTERVIEWING' }
    ]);
    renderComponent();

    await waitFor(() => expect(screen.getByText('Acme Corp')).toBeInTheDocument());

    const tableModeButton = screen.getByRole('button', { name: /table/i });
    fireEvent.click(tableModeButton);
    await waitFor(() => expect(screen.getByRole('table')).toBeInTheDocument());

    // Select first and second rows
    const checkboxes = screen.getAllByRole('checkbox').filter(cb => cb.getAttribute('aria-label') !== 'Select all applications');
    expect(checkboxes.length).toBe(3); // 3 applications

    fireEvent.click(checkboxes[0]);
    fireEvent.click(checkboxes[1]);

    // Check BatchActionToolbar appears
    const toolbar = screen.getByText(/2 applications selected/i);
    expect(toolbar).toBeInTheDocument();

    // Trigger batch update
    const statusSelect = screen.getByRole('combobox', { name: /batch status update/i });
    fireEvent.change(statusSelect, { target: { value: 'INTERVIEWING' } });

    const applyBtn = screen.getByRole('button', { name: /apply status/i });
    fireEvent.click(applyBtn);

    await waitFor(() => {
      expect(applicationService.batchUpdateStatus).toHaveBeenCalledWith([1, 2], 'INTERVIEWING');
    });

    // Toolbar should hide after selection clears
    await waitFor(() => {
      expect(screen.queryByText(/2 applications selected/i)).not.toBeInTheDocument();
    });
  });

  it('handles select all / indeterminate checkbox state', async () => {
    renderComponent();
    await waitFor(() => expect(screen.getByText('Acme Corp')).toBeInTheDocument());

    const tableModeButton = screen.getByRole('button', { name: /table/i });
    fireEvent.click(tableModeButton);
    await waitFor(() => expect(screen.getByRole('table')).toBeInTheDocument());

    const selectAllCheckbox = screen.getByRole('checkbox', { name: /select all/i });
    const rowCheckboxes = screen.getAllByRole('checkbox').filter(cb => cb.getAttribute('aria-label') !== 'Select all applications');

    // Click select all
    fireEvent.click(selectAllCheckbox);
    expect(screen.getByText(/3 applications selected/i)).toBeInTheDocument();
    rowCheckboxes.forEach(cb => expect(cb).toBeChecked());

    // Deselect one
    fireEvent.click(rowCheckboxes[0]);
    expect(screen.getByText(/2 applications selected/i)).toBeInTheDocument();
    expect(selectAllCheckbox).not.toBeChecked();
    expect(selectAllCheckbox).toHaveAttribute('aria-checked', 'mixed');

    // Deselect all from toolbar
    const deselectButton = screen.getByRole('button', { name: /deselect all/i });
    fireEvent.click(deselectButton);
    expect(screen.queryByText(/selected/i)).not.toBeInTheDocument();
  });

  it('prevents batch update requests when navigator.onLine is false', async () => {
    vi.stubGlobal('navigator', { onLine: false });
    applicationService.batchUpdateStatus.mockResolvedValue([]);
    
    renderComponent();
    await waitFor(() => expect(screen.getByText('Acme Corp')).toBeInTheDocument());

    const tableModeButton = screen.getByRole('button', { name: /table/i });
    fireEvent.click(tableModeButton);
    await waitFor(() => expect(screen.getByRole('table')).toBeInTheDocument());

    const checkboxes = screen.getAllByRole('checkbox').filter(cb => cb.getAttribute('aria-label') !== 'Select all applications');
    fireEvent.click(checkboxes[0]);

    const statusSelect = screen.getByRole('combobox', { name: /batch status update/i });
    fireEvent.change(statusSelect, { target: { value: 'REJECTED' } });

    const applyBtn = screen.getByRole('button', { name: /apply status/i });
    fireEvent.click(applyBtn);

    // Expect toast error, but service NOT called
    await waitFor(() => {
      expect(applicationService.batchUpdateStatus).not.toHaveBeenCalled();
    });
    
    expect(screen.getByText(/Cannot perform batch updates while offline/i)).toBeInTheDocument();
  });
});
