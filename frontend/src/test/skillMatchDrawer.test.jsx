import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { SkillMatchDrawer } from '../pages/Opportunities/components/SkillMatchDrawer';
import { opportunityService } from '../services/opportunityService';
import { ToastProvider } from '../context/ToastContext';

vi.mock('../services/opportunityService');
vi.mock('../services/skillService');

describe('SkillMatchDrawer ADR-06 Deterministic Logic Suite', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockOpportunity = {
    id: 1,
    title: 'Frontend Developer',
    companyName: 'Acme Corp',
  };

  const renderComponent = () => {
    return render(
      <ToastProvider>
        <SkillMatchDrawer
          isOpen={true}
          opportunity={mockOpportunity}
          onClose={vi.fn()}
        />
      </ToastProvider>
    );
  };

  it('handles Case A: Zero skills required', async () => {
    opportunityService.getOpportunitySkillMatch.mockResolvedValue({
      totalRequiredSkills: 0,
      totalPreferredSkills: 0,
      zeroSkillsSpecified: true,
      matchPercentage: null
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('100% Match (Zero Skills Required)')).toBeInTheDocument();
      expect(screen.queryByText(/NaN/)).not.toBeInTheDocument();
      expect(screen.queryByText(/undefined/)).not.toBeInTheDocument();
    });
  });

  it('handles Case B: Zero user skills matched', async () => {
    opportunityService.getOpportunitySkillMatch.mockResolvedValue({
      totalRequiredSkills: 2,
      totalPreferredSkills: 0,
      matchedRequiredSkills: [],
      missingRequiredSkills: ['React', 'TypeScript'],
      matchPercentage: 0
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('0% Match')).toBeInTheDocument();
      expect(screen.queryByText(/NaN/)).not.toBeInTheDocument();
    });
  });

  it('handles Case C: Partial match', async () => {
    opportunityService.getOpportunitySkillMatch.mockResolvedValue({
      totalRequiredSkills: 10,
      totalPreferredSkills: 0,
      matchedRequiredSkills: ['React', 'CSS', 'HTML', 'JS'],
      missingRequiredSkills: ['Node', 'SQL', 'Git', 'AWS', 'Docker', 'Jest'],
      matchPercentage: 40,
      matchLabel: 'Partial'
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('40% Match (Partial)')).toBeInTheDocument();
      expect(screen.queryByText(/NaN/)).not.toBeInTheDocument();
    });
  });

  it('handles Case D: Perfect match', async () => {
    opportunityService.getOpportunitySkillMatch.mockResolvedValue({
      totalRequiredSkills: 3,
      totalPreferredSkills: 0,
      matchedRequiredSkills: ['React', 'CSS', 'HTML'],
      missingRequiredSkills: [],
      matchPercentage: 100
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('100% Match (Perfect Alignment)')).toBeInTheDocument();
      expect(screen.queryByText(/NaN/)).not.toBeInTheDocument();
    });
  });
});
