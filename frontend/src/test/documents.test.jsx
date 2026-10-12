import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { DocumentModal } from '../pages/Documents/components/DocumentModal';
import { DocumentCard } from '../pages/Documents/components/DocumentCard';

describe('Documents Security & URL Protocol Suite', () => {
  const mockOnSave = vi.fn();
  const mockOnClose = vi.fn();
  const mockOnEdit = vi.fn();
  const mockOnDelete = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('DocumentModal URL Protocol Validation', () => {
    it('rejects non-http/https protocols (javascript:, file:)', async () => {
      render(
        <DocumentModal
          isOpen={true}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      );

      // Fill in required fields
      fireEvent.change(screen.getByLabelText(/document title/i), { target: { value: 'My Resume' } });

      // Try malicious javascript: protocol
      const urlInput = screen.getByLabelText(/external storage url/i);
      fireEvent.change(urlInput, { target: { value: 'javascript:alert(1)' } });

      const submitBtn = screen.getByRole('button', { name: /register document/i });
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(screen.getByText(/Storage path must be a valid absolute HTTP or HTTPS URL/i)).toBeInTheDocument();
        expect(mockOnSave).not.toHaveBeenCalled();
      });

      // Try file: protocol
      fireEvent.change(urlInput, { target: { value: 'file:///etc/passwd' } });
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(screen.getByText(/Storage path must be a valid absolute HTTP or HTTPS URL/i)).toBeInTheDocument();
        expect(mockOnSave).not.toHaveBeenCalled();
      });
    });

    it('accepts valid http/https URLs', async () => {
      render(
        <DocumentModal
          isOpen={true}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      );

      fireEvent.change(screen.getByLabelText(/document title/i), { target: { value: 'My Resume' } });
      const urlInput = screen.getByLabelText(/external storage url/i);
      fireEvent.change(urlInput, { target: { value: 'https://drive.google.com/test' } });

      const submitBtn = screen.getByRole('button', { name: /register document/i });
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(mockOnSave).toHaveBeenCalledWith({
          title: 'My Resume',
          documentType: 'RESUME',
          storagePath: 'https://drive.google.com/test',
          versionTag: null
        }, false, undefined);
      });
    });
  });

  describe('DocumentCard Link Security Attributes', () => {
    it('applies target="_blank" and rel="noopener noreferrer" to external links', () => {
      const mockDoc = {
        id: 1,
        title: 'Backend Resume',
        documentType: 'RESUME',
        storagePath: 'https://example.com/resume.pdf',
        versionTag: 'v1'
      };

      render(
        <DocumentCard
          document={mockDoc}
          onEdit={mockOnEdit}
          onDelete={mockOnDelete}
        />
      );

      const link = screen.getByRole('link', { name: /open external link/i });
      expect(link).toHaveAttribute('href', 'https://example.com/resume.pdf');
      expect(link).toHaveAttribute('target', '_blank');
      expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    });
  });
});
