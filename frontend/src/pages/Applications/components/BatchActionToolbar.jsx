import React, { useState } from 'react';
import { Button } from '../../../components/common/Button';
import './BatchActionToolbar.css';

const STAGES = [
  { value: 'SAVED', label: 'Saved' },
  { value: 'APPLIED', label: 'Applied' },
  { value: 'SCREENING', label: 'Screening' },
  { value: 'INTERVIEWING', label: 'Interviewing' },
  { value: 'OFFER', label: 'Offer' },
  { value: 'ACCEPTED', label: 'Accepted' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'WITHDRAWN', label: 'Withdrawn' },
];

export const BatchActionToolbar = ({
  selectedCount,
  onBatchUpdateStatus,
  onDeselectAll,
  isUpdating,
}) => {
  const [selectedStage, setSelectedStage] = useState('APPLIED');

  if (selectedCount === 0) {
    return null;
  }

  const handleApply = () => {
    onBatchUpdateStatus(selectedStage);
  };

  return (
    <div className="batch-action-toolbar">
      <div className="batch-left">
        <span className="batch-count-pill">{selectedCount}</span>
        <span className="batch-title">
          {selectedCount === 1 ? '1 application selected' : `${selectedCount} applications selected`}
        </span>
      </div>

      <div className="batch-actions">
        <label
          htmlFor="batch-status-select-input"
          style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)' }}
        >
          Move to:
        </label>
        <select
          id="batch-status-select-input"
          className="batch-status-select"
          value={selectedStage}
          onChange={(e) => setSelectedStage(e.target.value)}
          aria-label="Select stage for batch status update"
          disabled={isUpdating}
        >
          {STAGES.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>

        <Button
          variant="primary"
          size="sm"
          onClick={handleApply}
          isLoading={isUpdating}
          disabled={isUpdating}
        >
          Apply Status
        </Button>

        <Button
          variant="secondary"
          size="sm"
          onClick={onDeselectAll}
          disabled={isUpdating}
        >
          Deselect All
        </Button>
      </div>
    </div>
  );
};