import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { InterviewPrepDrawer } from '../pages/Interviews/components/InterviewPrepDrawer';
import { ToastProvider } from '../context/ToastContext';

describe('InterviewPrepDrawer Outcome Workflow Suite', () => {
  const mockOnRecordOutcome = vi.fn();
  const mockOnEdit = vi.fn();
  const mockOnClose = vi.fn();

  const mockApplication = {
    id: 1,
    companyName: 'Acme Corp',
    opportunityTitle: 'Frontend Intern'
  };

  const mockInterview = {
    id: 101,
    roundTitle: 'Initial Screening',
    roundType: 'RECRUITER_SCREEN',
    roundNumber: 1,
    outcome: 'PENDING',
    reflectionNotes: '',
    applicationId: 1
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderComponent = (interviewOverride = {}) => {
    return render(
      <ToastProvider>
        <InterviewPrepDrawer
          isOpen={true}
          application={mockApplication}
          interview={{ ...mockInterview, ...interviewOverride }}
          onRecordOutcome={mockOnRecordOutcome}
          onEdit={mockOnEdit}
          onClose={mockOnClose}
        />
      </ToastProvider>
    );
  };

  it('renders initial state correctly', () => {
    renderComponent();
    expect(screen.getByText('Initial Screening')).toBeInTheDocument();
    
    const pendingBtn = screen.getByRole('button', { name: /pending/i });
    expect(pendingBtn).toHaveAttribute('aria-pressed', 'true');
  });

  it('handles outcome selection and submission', async () => {
    mockOnRecordOutcome.mockResolvedValueOnce({});
    renderComponent();

    // Select 'PASSED'
    const passedBtn = screen.getByRole('button', { name: /passed/i });
    fireEvent.click(passedBtn);
    expect(passedBtn).toHaveAttribute('aria-pressed', 'true');

    // Add reflection
    const textarea = screen.getByRole('textbox', { name: /interview reflections/i });
    fireEvent.change(textarea, { target: { value: 'Went well, asked about React context.' } });

    // Submit
    const saveBtn = screen.getByRole('button', { name: /save outcome/i });
    fireEvent.click(saveBtn);

    expect(saveBtn).toBeDisabled();

    await waitFor(() => {
      expect(mockOnRecordOutcome).toHaveBeenCalledWith(
        101, 
        'PASSED', 
        'Went well, asked about React context.'
      );
    });
  });

  it('initializes with existing outcome and notes', () => {
    renderComponent({
      outcome: 'FAILED',
      reflectionNotes: 'Need to study dynamic programming more.'
    });

    const failedBtn = screen.getByRole('button', { name: /failed/i });
    expect(failedBtn).toHaveAttribute('aria-pressed', 'true');

    const textarea = screen.getByRole('textbox', { name: /interview reflections/i });
    expect(textarea).toHaveValue('Need to study dynamic programming more.');
  });
});
