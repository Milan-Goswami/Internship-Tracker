import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, fireEvent } from '@testing-library/react';
import { ReminderModal } from '../pages/Reminders/components/ReminderModal';
import { DeleteConfirmModal } from '../pages/Applications/components/DeleteConfirmModal';
import { InterviewModal } from '../pages/Interviews/components/InterviewModal';

describe('Modal Escape Dismissal Suite', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('ReminderModal', () => {
    it('calls onClose when Escape key is pressed while open', () => {
      const handleClose = vi.fn();
      render(
        <ReminderModal
          isOpen={true}
          onClose={handleClose}
          onSave={vi.fn()}
          applications={[]}
        />
      );

      fireEvent.keyDown(window, { key: 'Escape' });
      expect(handleClose).toHaveBeenCalledTimes(1);
    });

    it('does NOT call onClose on other keys such as Enter, Space, or Tab', () => {
      const handleClose = vi.fn();
      render(
        <ReminderModal
          isOpen={true}
          onClose={handleClose}
          onSave={vi.fn()}
          applications={[]}
        />
      );

      fireEvent.keyDown(window, { key: 'Enter' });
      fireEvent.keyDown(window, { key: ' ' });
      fireEvent.keyDown(window, { key: 'Tab' });
      fireEvent.keyDown(window, { key: 'ArrowDown' });

      expect(handleClose).not.toHaveBeenCalled();
    });

    it('removes the keydown listener when unmounted', () => {
      const removeSpy = vi.spyOn(window, 'removeEventListener');
      const { unmount } = render(
        <ReminderModal
          isOpen={true}
          onClose={vi.fn()}
          onSave={vi.fn()}
          applications={[]}
        />
      );

      unmount();
      expect(removeSpy).toHaveBeenCalledWith('keydown', expect.any(Function));
    });
  });

  describe('DeleteConfirmModal', () => {
    it('calls onClose when Escape key is pressed', () => {
      const handleClose = vi.fn();
      render(
        <DeleteConfirmModal
          isOpen={true}
          onClose={handleClose}
          onConfirm={vi.fn()}
          application={{ id: 1, companyName: 'Datadog', opportunityTitle: 'Intern' }}
          isDeleting={false}
        />
      );

      fireEvent.keyDown(window, { key: 'Escape' });
      expect(handleClose).toHaveBeenCalledTimes(1);
    });
  });

  describe('InterviewModal', () => {
    it('calls onClose when Escape key is pressed', () => {
      const handleClose = vi.fn();
      render(
        <InterviewModal
          isOpen={true}
          onClose={handleClose}
          onSave={vi.fn()}
          applications={[{ id: 1, companyName: 'Google', opportunityTitle: 'SWE' }]}
        />
      );

      fireEvent.keyDown(window, { key: 'Escape' });
      expect(handleClose).toHaveBeenCalledTimes(1);
    });
  });
});
