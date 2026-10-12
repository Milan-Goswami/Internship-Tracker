import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { ApplicationsTable } from '../pages/Applications/components/ApplicationsTable';
import { ApplicationsWorkspace } from '../pages/Applications/ApplicationsPage';
import { CommandBar } from '../components/navigation/CommandBar';
import { AuthContext } from '../context/AuthContext';
import { ToastProvider } from '../context/ToastContext';
import { applicationService } from '../services/applicationService';
import { opportunityService } from '../services/opportunityService';

vi.mock('../services/opportunityService', () => ({
  opportunityService: {
    getOpportunities: vi.fn().mockResolvedValue([]),
    getOpportunityById: vi.fn().mockResolvedValue(null),
    createOpportunity: vi.fn(),
  },
}));

vi.mock('../services/applicationService', () => ({
  applicationService: {
    getApplications: vi.fn(),
    createApplication: vi.fn(),
    updateApplication: vi.fn(),
    updateApplicationStatus: vi.fn(),
    batchUpdateStatus: vi.fn(),
    deleteApplication: vi.fn(),
  },
}));

const mockApplications = [
  {
    id: 1,
    companyName: 'Acme Corp',
    opportunityTitle: 'Frontend Engineer Intern',
    status: 'APPLIED',
    userRating: 4,
    appliedAt: '2026-09-01T12:00:00Z',
    savedAt: '2026-08-30T10:00:00Z',
    personalNotes: 'Referral through alum',
  },
  {
    id: 2,
    companyName: 'Beta Tech',
    opportunityTitle: 'Backend Engineer Intern',
    status: 'INTERVIEWING',
    userRating: 5,
    appliedAt: '2026-09-05T12:00:00Z',
    savedAt: '2026-09-02T10:00:00Z',
    personalNotes: 'Completed take-home test',
  },
  {
    id: 3,
    companyName: 'Gamma Labs',
    opportunityTitle: 'Fullstack Intern',
    status: 'OFFER',
    userRating: 3,
    appliedAt: '2026-09-10T12:00:00Z',
    savedAt: '2026-09-08T10:00:00Z',
    personalNotes: 'Offer expires in 2 weeks',
  },
];

const renderApplicationsTable = (overrides = {}) => {
  const defaultProps = {
    applications: mockApplications,
    selectedIds: [],
    onToggleSelect: vi.fn(),
    onToggleSelectAll: vi.fn(),
    sortField: 'date',
    sortOrder: 'desc',
    onSort: vi.fn(),
    onStatusChange: vi.fn(),
    onEdit: vi.fn(),
    onDelete: vi.fn(),
    ...overrides,
  };

  const result = render(<ApplicationsTable {...defaultProps} />);
  return { ...result, props: defaultProps };
};

const renderApplicationsWorkspace = () => {
  const authMock = {
    user: { id: 1, fullName: 'Alice Candidate', email: 'alice@example.com' },
    isAuthenticated: true,
    logout: vi.fn(),
  };

  return render(
    <BrowserRouter>
      <AuthContext.Provider value={authMock}>
        <ToastProvider>
          <ApplicationsWorkspace />
        </ToastProvider>
      </AuthContext.Provider>
    </BrowserRouter>
  );
};

const renderCommandBar = (overrides = {}) => {
  const authMock = {
    user: { fullName: 'Alice Candidate', email: 'alice@example.com' },
    isAuthenticated: true,
    logout: vi.fn(),
  };

  return render(
    <BrowserRouter>
      <AuthContext.Provider value={authMock}>
        <CommandBar {...overrides} />
      </AuthContext.Provider>
    </BrowserRouter>
  );
};

describe('M12.1 — Keyboard Navigation & Accessibility Suite', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  describe('1. Applications Table — j / k and Arrow Navigation', () => {
    it('sets initial roving tabindex=0 on the first row and -1 on subsequent rows', () => {
      renderApplicationsTable();
      const rows = screen.getAllByRole('row').slice(1); // Exclude header row
      expect(rows).toHaveLength(3);

      expect(rows[0]).toHaveAttribute('tabIndex', '0');
      expect(rows[0]).toHaveClass('active-row');
      expect(rows[1]).toHaveAttribute('tabIndex', '-1');
      expect(rows[2]).toHaveAttribute('tabIndex', '-1');
    });

    it('navigates down using ArrowDown and j keys', () => {
      renderApplicationsTable();
      const rows = screen.getAllByRole('row').slice(1);

      // Press ArrowDown on row 0
      fireEvent.keyDown(rows[0], { key: 'ArrowDown' });
      expect(rows[1]).toHaveClass('active-row');
      expect(rows[1]).toHaveAttribute('tabIndex', '0');
      expect(rows[0]).toHaveAttribute('tabIndex', '-1');

      // Press j on row 1
      fireEvent.keyDown(rows[1], { key: 'j' });
      expect(rows[2]).toHaveClass('active-row');
      expect(rows[2]).toHaveAttribute('tabIndex', '0');
      expect(rows[1]).toHaveAttribute('tabIndex', '-1');
    });

    it('navigates up using ArrowUp and k keys', () => {
      renderApplicationsTable();
      const rows = screen.getAllByRole('row').slice(1);

      // Navigate down to row 2
      fireEvent.keyDown(rows[0], { key: 'j' });
      fireEvent.keyDown(rows[1], { key: 'j' });
      expect(rows[2]).toHaveClass('active-row');

      // Press ArrowUp on row 2
      fireEvent.keyDown(rows[2], { key: 'ArrowUp' });
      expect(rows[1]).toHaveClass('active-row');
      expect(rows[1]).toHaveAttribute('tabIndex', '0');

      // Press k on row 1
      fireEvent.keyDown(rows[1], { key: 'k' });
      expect(rows[0]).toHaveClass('active-row');
      expect(rows[0]).toHaveAttribute('tabIndex', '0');
    });

    it('clamps navigation at boundaries (first row and last row)', () => {
      renderApplicationsTable();
      const rows = screen.getAllByRole('row').slice(1);

      // Clamping at first row
      fireEvent.keyDown(rows[0], { key: 'ArrowUp' });
      expect(rows[0]).toHaveClass('active-row');
      fireEvent.keyDown(rows[0], { key: 'k' });
      expect(rows[0]).toHaveClass('active-row');

      // Navigate to last row
      fireEvent.keyDown(rows[0], { key: 'ArrowDown' });
      fireEvent.keyDown(rows[1], { key: 'ArrowDown' });
      expect(rows[2]).toHaveClass('active-row');

      // Clamping at last row
      fireEvent.keyDown(rows[2], { key: 'ArrowDown' });
      expect(rows[2]).toHaveClass('active-row');
      fireEvent.keyDown(rows[2], { key: 'j' });
      expect(rows[2]).toHaveClass('active-row');
    });

    it('updates active row when row is clicked or receives focus', () => {
      renderApplicationsTable();
      const rows = screen.getAllByRole('row').slice(1);

      fireEvent.click(rows[2]);
      expect(rows[2]).toHaveClass('active-row');
      expect(rows[2]).toHaveAttribute('tabIndex', '0');
      expect(rows[0]).toHaveAttribute('tabIndex', '-1');

      fireEvent.focus(rows[1]);
      expect(rows[1]).toHaveClass('active-row');
      expect(rows[1]).toHaveAttribute('tabIndex', '0');
    });

    it('handles empty applications list without errors', () => {
      const { container } = renderApplicationsTable({ applications: [] });
      const table = container.querySelector('table');
      expect(table).toBeInTheDocument();
      const tbodyRows = container.querySelectorAll('tbody tr');
      expect(tbodyRows).toHaveLength(0);
    });

    it('clamps active index when applications list shrinks', () => {
      const { rerender } = renderApplicationsTable();
      const rows = screen.getAllByRole('row').slice(1);

      // Move to index 2 (Gamma Labs)
      fireEvent.keyDown(rows[0], { key: 'j' });
      fireEvent.keyDown(rows[1], { key: 'j' });
      expect(rows[2]).toHaveClass('active-row');

      // Re-render with only 1 application
      rerender(
        <ApplicationsTable
          applications={[mockApplications[0]]}
          selectedIds={[]}
          onToggleSelect={vi.fn()}
          onToggleSelectAll={vi.fn()}
          sortField="date"
          sortOrder="desc"
          onSort={vi.fn()}
          onStatusChange={vi.fn()}
          onEdit={vi.fn()}
          onDelete={vi.fn()}
        />
      );

      const updatedRows = screen.getAllByRole('row').slice(1);
      expect(updatedRows).toHaveLength(1);
      expect(updatedRows[0]).toHaveClass('active-row');
      expect(updatedRows[0]).toHaveAttribute('tabIndex', '0');
    });
  });

  describe('2. Row Activation (Enter and Space)', () => {
    it('activates the highlighted row and calls onEdit when Enter is pressed on the row', () => {
      const onEdit = vi.fn();
      renderApplicationsTable({ onEdit });
      const rows = screen.getAllByRole('row').slice(1);

      // Press Enter on row 0
      fireEvent.keyDown(rows[0], { key: 'Enter' });
      expect(onEdit).toHaveBeenCalledTimes(1);
      expect(onEdit).toHaveBeenCalledWith(mockApplications[0]);

      // Move to row 1 and press Enter
      fireEvent.keyDown(rows[0], { key: 'j' });
      fireEvent.keyDown(rows[1], { key: 'Enter' });
      expect(onEdit).toHaveBeenCalledTimes(2);
      expect(onEdit).toHaveBeenCalledWith(mockApplications[1]);
    });

    it('toggles row selection when Space is pressed on the row itself', () => {
      const onToggleSelect = vi.fn();
      renderApplicationsTable({ onToggleSelect });
      const rows = screen.getAllByRole('row').slice(1);

      fireEvent.keyDown(rows[0], { key: ' ' });
      expect(onToggleSelect).toHaveBeenCalledTimes(1);
      expect(onToggleSelect).toHaveBeenCalledWith(mockApplications[0].id);
    });

    it('does NOT hijack Enter when pressed on interactive buttons', () => {
      const onEdit = vi.fn();
      renderApplicationsTable({ onEdit });
      const editButtons = screen.getAllByTitle('Edit application');

      // KeyDown on button does not trigger row-level onEdit handler (native button handles it)
      fireEvent.keyDown(editButtons[0], { key: 'Enter' });
      expect(onEdit).not.toHaveBeenCalled();
    });

    it('does NOT hijack Space when pressed on interactive checkbox', () => {
      const onToggleSelect = vi.fn();
      renderApplicationsTable({ onToggleSelect });
      const checkboxes = screen.getAllByRole('checkbox').slice(1); // row checkboxes

      fireEvent.keyDown(checkboxes[0], { key: ' ' });
      // Row-level onToggleSelect is skipped; native checkbox handles change
      expect(onToggleSelect).not.toHaveBeenCalled();
    });

    it('does NOT intercept ArrowDown/ArrowUp when inside status select dropdown', () => {
      renderApplicationsTable();
      const rows = screen.getAllByRole('row').slice(1);
      const selects = screen.getAllByRole('combobox');

      expect(rows[0]).toHaveClass('active-row');

      // Press ArrowDown inside select
      fireEvent.keyDown(selects[0], { key: 'ArrowDown' });
      // Row must remain on row 0, not navigate to row 1
      expect(rows[0]).toHaveClass('active-row');
    });
  });

  describe('3. Global Shortcuts (/, Ctrl+K, Cmd+K, Escape)', () => {
    it('focuses global search when / is pressed outside editable fields', () => {
      renderCommandBar();
      const searchInput = screen.getByLabelText(/global search/i);
      expect(document.activeElement).not.toBe(searchInput);

      fireEvent.keyDown(window, { key: '/' });
      expect(document.activeElement).toBe(searchInput);
    });

    it('focuses global search when Ctrl+K is pressed', () => {
      renderCommandBar();
      const searchInput = screen.getByLabelText(/global search/i);

      fireEvent.keyDown(window, { key: 'k', ctrlKey: true });
      expect(document.activeElement).toBe(searchInput);
    });

    it('focuses global search when Cmd+K is pressed (macOS)', () => {
      renderCommandBar();
      const searchInput = screen.getByLabelText(/global search/i);

      fireEvent.keyDown(window, { key: 'k', metaKey: true });
      expect(document.activeElement).toBe(searchInput);
    });

    it('does NOT focus search when / is pressed inside input or textarea', () => {
      render(
        <div>
          <input data-testid="test-input" type="text" />
          <textarea data-testid="test-textarea" />
          <BrowserRouter>
            <AuthContext.Provider value={{ user: null, isAuthenticated: false, logout: vi.fn() }}>
              <CommandBar />
            </AuthContext.Provider>
          </BrowserRouter>
        </div>
      );

      const input = screen.getByTestId('test-input');
      const textarea = screen.getByTestId('test-textarea');
      const searchInput = screen.getByLabelText(/global search/i);

      input.focus();
      fireEvent.keyDown(input, { key: '/' });
      expect(document.activeElement).toBe(input);
      expect(document.activeElement).not.toBe(searchInput);

      textarea.focus();
      fireEvent.keyDown(textarea, { key: '/' });
      expect(document.activeElement).toBe(textarea);
      expect(document.activeElement).not.toBe(searchInput);
    });

    it('does NOT intercept / or Ctrl+K when a modal is active in the document', () => {
      render(
        <div>
          <div className="modal-overlay" role="dialog" aria-modal="true">
            <button data-testid="modal-btn">Modal Action</button>
          </div>
          <BrowserRouter>
            <AuthContext.Provider value={{ user: null, isAuthenticated: false, logout: vi.fn() }}>
              <CommandBar />
            </AuthContext.Provider>
          </BrowserRouter>
        </div>
      );

      const searchInput = screen.getByLabelText(/global search/i);
      fireEvent.keyDown(window, { key: '/' });
      expect(document.activeElement).not.toBe(searchInput);

      fireEvent.keyDown(window, { key: 'k', ctrlKey: true });
      expect(document.activeElement).not.toBe(searchInput);
    });

    it('blurs search input when Escape is pressed', () => {
      renderCommandBar();
      const searchInput = screen.getByLabelText(/global search/i);

      searchInput.focus();
      expect(document.activeElement).toBe(searchInput);

      fireEvent.keyDown(searchInput, { key: 'Escape' });
      expect(document.activeElement).not.toBe(searchInput);
    });

    it('respects WCAG SC 2.1.4 single-key shortcut disable setting for /', () => {
      localStorage.setItem('interntrack_disable_single_key_shortcuts', 'true');
      renderCommandBar();
      const searchInput = screen.getByLabelText(/global search/i);

      fireEvent.keyDown(window, { key: '/' });
      expect(document.activeElement).not.toBe(searchInput);

      // Modifier shortcut Ctrl+K should still work even if single-key is disabled!
      fireEvent.keyDown(window, { key: 'k', ctrlKey: true });
      expect(document.activeElement).toBe(searchInput);
    });
  });

  describe('4. Global n Shortcut for New Application Modal', () => {
    it('opens New Application modal when n is pressed outside form fields', async () => {
      applicationService.getApplications.mockResolvedValueOnce(mockApplications);
      renderApplicationsWorkspace();

      await waitFor(() => {
        expect(screen.getByText('Acme Corp')).toBeInTheDocument();
      });

      // No modal open initially
      expect(screen.queryByRole('heading', { name: /add application/i })).not.toBeInTheDocument();

      // Press 'n' on window
      fireEvent.keyDown(window, { key: 'n' });

      // Modal should open
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /add application/i })).toBeInTheDocument();
      });
    });

    it('does NOT open New Application modal when modifier keys are used (Ctrl+N, Alt+N, Meta+N)', async () => {
      applicationService.getApplications.mockResolvedValueOnce(mockApplications);
      renderApplicationsWorkspace();

      await waitFor(() => {
        expect(screen.getByText('Acme Corp')).toBeInTheDocument();
      });

      fireEvent.keyDown(window, { key: 'n', ctrlKey: true });
      fireEvent.keyDown(window, { key: 'n', altKey: true });
      fireEvent.keyDown(window, { key: 'n', metaKey: true });

      expect(screen.queryByRole('heading', { name: /add application/i })).not.toBeInTheDocument();
    });

    it('does NOT trigger n shortcut when typing inside an input', async () => {
      applicationService.getApplications.mockResolvedValueOnce(mockApplications);
      renderApplicationsWorkspace();

      await waitFor(() => {
        expect(screen.getByText('Acme Corp')).toBeInTheDocument();
      });

      const searchInput = screen.getByPlaceholderText(/search by company, role, or notes/i);
      searchInput.focus();
      fireEvent.keyDown(searchInput, { key: 'n' });

      expect(screen.queryByRole('heading', { name: /add application/i })).not.toBeInTheDocument();
    });

    it('does NOT re-trigger n shortcut when modal is already open', async () => {
      applicationService.getApplications.mockResolvedValueOnce(mockApplications);
      renderApplicationsWorkspace();

      await waitFor(() => {
        expect(screen.getByText('Acme Corp')).toBeInTheDocument();
      });

      // Open modal with 'n'
      fireEvent.keyDown(window, { key: 'n' });
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /add application/i })).toBeInTheDocument();
      });

      // Press 'n' again while modal is open - should not duplicate or error
      fireEvent.keyDown(window, { key: 'n' });
      expect(screen.getAllByRole('heading', { name: /add application/i })).toHaveLength(1);
    });

    it('respects WCAG SC 2.1.4 single-key disable setting for n shortcut', async () => {
      localStorage.setItem('interntrack_disable_single_key_shortcuts', 'true');
      applicationService.getApplications.mockResolvedValueOnce(mockApplications);
      renderApplicationsWorkspace();

      await waitFor(() => {
        expect(screen.getByText('Acme Corp')).toBeInTheDocument();
      });

      fireEvent.keyDown(window, { key: 'n' });
      expect(screen.queryByRole('heading', { name: /add application/i })).not.toBeInTheDocument();
    });
  });

  describe('5. Listener Cleanup on Unmount', () => {
    it('cleans up CommandBar keydown listener on unmount', () => {
      const removeSpy = vi.spyOn(window, 'removeEventListener');
      const { unmount } = renderCommandBar();

      unmount();
      expect(removeSpy).toHaveBeenCalledWith('keydown', expect.any(Function));
    });

    it('cleans up ApplicationsWorkspace keydown listener on unmount', async () => {
      applicationService.getApplications.mockResolvedValueOnce([]);
      const removeSpy = vi.spyOn(window, 'removeEventListener');
      const { unmount } = renderApplicationsWorkspace();

      unmount();
      expect(removeSpy).toHaveBeenCalledWith('keydown', expect.any(Function));
    });
  });
});
