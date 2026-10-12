import React from 'react';
import { Button } from '../../../components/common/Button';
import './OpportunityCard.css';

const formatStipend = (amount, currency = 'USD', period = 'HOURLY') => {
  if (!amount) return null;
  const num = Number(amount);
  const symbol = currency === 'USD' ? '$' : currency + ' ';
  let suffix = '';
  if (period === 'HOURLY') suffix = '/hr';
  else if (period === 'MONTHLY') suffix = '/mo';
  else if (period === 'TOTAL') suffix = ' total';

  return `${symbol}${num.toLocaleString()}${suffix}`;
};

export const OpportunityCard = ({
  opportunity,
  pipelineApplication,
  skillMatch,
  onOpenDrawer,
  onConvert,
  onEdit,
  onDelete,
  isConverting,
}) => {
  const stipendText = formatStipend(
    opportunity.stipendAmount,
    opportunity.stipendCurrency,
    opportunity.stipendPeriod
  );

  const getMatchTier = (match) => {
    if (!match) return { class: 'match-none', label: 'Analyze Match' };
    if (
      match.zeroSkillsSpecified ||
      (match.totalRequiredSkills === 0 && match.totalPreferredSkills === 0) ||
      match.matchPercentage == null
    ) {
      return { class: 'match-high', label: '100% Match' };
    }
    const raw = typeof match.matchPercentage === 'number' ? match.matchPercentage : 0;
    const pct = Math.max(0, Math.min(100, Math.round(raw)));
    if (pct >= 70) return { class: 'match-high', label: `${pct}% Match` };
    if (pct >= 40) return { class: 'match-medium', label: `${pct}% Match` };
    return { class: 'match-low', label: `${pct}% Match` };
  };

  const matchTier = getMatchTier(skillMatch);
  const companyInitial = (opportunity.companyName || 'C').charAt(0).toUpperCase();

  return (
    <div className="opportunity-card">
      <div>
        <div className="opportunity-card-header">
          <div className="opportunity-company-info">
            <div className="opportunity-company-logo">
              {opportunity.companyLogoUrl ? (
                <img src={opportunity.companyLogoUrl} alt={opportunity.companyName} />
              ) : (
                <span>{companyInitial}</span>
              )}
            </div>
            <div style={{ minWidth: 0 }}>
              <div className="opportunity-company-name">{opportunity.companyName || 'Unknown Company'}</div>
              <h2
                className="opportunity-title"
                onClick={() => onOpenDrawer(opportunity)}
                title="View details & skill match"
              >
                {opportunity.title}
              </h2>
            </div>
          </div>

          <div className="opportunity-card-badges">
            {opportunity.isCustom && <span className="badge-custom">Custom</span>}
          </div>
        </div>

        <div className="opportunity-meta-tags">
          {opportunity.locationType && (
            <span className="meta-chip">
              📍 {opportunity.locationType}
              {opportunity.locationCity ? ` (${opportunity.locationCity})` : ''}
            </span>
          )}
          {stipendText && <span className="meta-chip stipend">💰 {stipendText}</span>}
          {opportunity.durationMonths && (
            <span className="meta-chip">⏱ {opportunity.durationMonths} mo</span>
          )}
        </div>

        <div
          className="opportunity-skill-match-banner"
          onClick={() => onOpenDrawer(opportunity)}
          role="button"
          tabIndex={0}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              onOpenDrawer(opportunity);
            }
          }}
          aria-label={`Skill match for ${opportunity.title}`}
        >
          <div className={`match-score-pill ${matchTier.class}`}>
            <span className="match-dot" />
            <span>{matchTier.label}</span>
          </div>
          <span className="match-cta">Details & Skills →</span>
        </div>
      </div>

      <div className="opportunity-card-actions">
        {pipelineApplication ? (
          <span className="pipeline-status-badge">
            ✓ In Pipeline: {pipelineApplication.status}
          </span>
        ) : (
          <Button
            variant="primary"
            size="sm"
            onClick={() => onConvert(opportunity)}
            isLoading={isConverting}
          >
            + Track Application
          </Button>
        )}

        <div className="card-owner-actions">
          <Button
            variant="secondary"
            size="sm"
            onClick={() => onOpenDrawer(opportunity)}
          >
            Overview
          </Button>

          {opportunity.isCustom && (
            <>
              <button
                className="card-icon-btn"
                onClick={() => onEdit(opportunity)}
                title="Edit Opportunity"
                aria-label="Edit Opportunity"
              >
                ✎
              </button>
              <button
                className="card-icon-btn delete"
                onClick={() => onDelete(opportunity)}
                title="Delete Opportunity"
                aria-label="Delete Opportunity"
              >
                ✕
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
};
