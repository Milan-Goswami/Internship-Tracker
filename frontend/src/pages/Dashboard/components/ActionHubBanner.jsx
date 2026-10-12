import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../../../components/common/Button';
import './ActionHubBanner.css';

export const ActionHubBanner = ({ urgentItems = [] }) => {
  const navigate = useNavigate();

  if (!urgentItems || urgentItems.length === 0) {
    return null;
  }

  const primaryItem = urgentItems[0];

  return (
    <div className="action-hub-banner">
      <div className="action-hub-left">
        <div className="action-hub-icon-box">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
            <line x1="12" y1="9" x2="12" y2="13" />
            <line x1="12" y1="17" x2="12.01" y2="17" />
          </svg>
        </div>
        <div>
          <div className="action-hub-title">Action Required Today</div>
          <div className="action-hub-desc">
            {primaryItem.title}
            {urgentItems.length > 1 && ` (+ ${urgentItems.length - 1} other impending items)`}
          </div>
        </div>
      </div>

      <Button
        variant="primary"
        size="sm"
        onClick={() => navigate(primaryItem.link || '/reminders')}
      >
        {primaryItem.actionLabel || 'View Action'}
      </Button>
    </div>
  );
};
