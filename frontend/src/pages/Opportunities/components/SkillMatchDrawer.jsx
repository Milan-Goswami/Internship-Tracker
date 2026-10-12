import React, { useEffect, useState, useCallback } from 'react';
import { Button } from '../../../components/common/Button';
import { Skeleton } from '../../../components/common/Skeleton';
import { opportunityService } from '../../../services/opportunityService';
import { skillService } from '../../../services/skillService';
import { useToast } from '../../../context/ToastContext';
import './SkillMatchDrawer.css';

export const SkillMatchDrawer = ({
  opportunity,
  isOpen,
  onClose,
  pipelineApplication,
  onConvert,
  isConverting,
  onSkillsUpdated,
}) => {
  const [skillMatch, setSkillMatch] = useState(null);
  const [isLoadingMatch, setIsLoadingMatch] = useState(false);
  const [matchError, setMatchError] = useState(null);
  const [addingSkillName, setAddingSkillName] = useState(null);

  const { showToast } = useToast();

  const fetchSkillMatch = useCallback(async () => {
    if (!opportunity?.id) return;
    setIsLoadingMatch(true);
    setMatchError(null);
    try {
      const data = await opportunityService.getOpportunitySkillMatch(opportunity.id);
      setSkillMatch(data);
    } catch (err) {
      setMatchError(err.response?.data?.message || 'Failed to compute skill match');
    } finally {
      setIsLoadingMatch(false);
    }
  }, [opportunity?.id]);

  useEffect(() => {
    let isCancelled = false;
    if (isOpen && opportunity?.id) {
      setIsLoadingMatch(true);
      setMatchError(null);
      opportunityService
        .getOpportunitySkillMatch(opportunity.id)
        .then((data) => {
          if (!isCancelled) {
            setSkillMatch(data);
            setIsLoadingMatch(false);
          }
        })
        .catch((err) => {
          if (!isCancelled) {
            setMatchError(err.response?.data?.message || 'Failed to compute skill match');
            setIsLoadingMatch(false);
          }
        });
    } else {
      setSkillMatch(null);
      setIsLoadingMatch(false);
      setMatchError(null);
    }

    return () => {
      isCancelled = true;
    };
  }, [isOpen, opportunity?.id]);

  // Handle ESC key to close
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  const handleAddSkillToProfile = async (skillName) => {
    if (typeof navigator !== 'undefined' && !navigator.onLine) {
      showToast('Cannot add skills while offline', 'warning');
      return;
    }
    setAddingSkillName(skillName);
    try {
      // 1. Find skill ID by searching canonical name
      const searchResults = await skillService.searchSkills(skillName);
      const match = searchResults?.find(
        (s) => s.name.toLowerCase() === skillName.toLowerCase() || s.canonicalName === skillName.toLowerCase()
      ) || searchResults?.[0];

      if (!match) {
        showToast(`Could not locate canonical skill ID for "${skillName}"`, 'error');
        return;
      }

      // 2. Add skill to user profile
      await skillService.addUserSkill({
        skillId: match.id,
        proficiencyLevel: 'INTERMEDIATE',
        yearsOfExperience: 1,
      });

      showToast(`Added ${skillName} to your skill profile!`, 'success');

      // 3. Re-fetch match to immediately reflect update
      await fetchSkillMatch();
      if (onSkillsUpdated) {
        onSkillsUpdated();
      }
    } catch (err) {
      showToast(err.response?.data?.message || `Failed to add ${skillName}`, 'error');
    } finally {
      setAddingSkillName(null);
    }
  };

  if (!isOpen || !opportunity) return null;

  // ADR-06 Deterministic Presentation Engine (Cases A–D)
  const getSkillMatchPresentation = (match) => {
    if (!match) {
      return { scorePct: 0, displayPct: '0%', badgeClass: 'badge-low', fillColor: '#f87171', label: 'Analyzing...' };
    }

    // Case A: Zero skills required
    const isZeroSkills = Boolean(
      match.zeroSkillsSpecified ||
      (match.totalRequiredSkills === 0 && match.totalPreferredSkills === 0) ||
      match.matchPercentage == null
    );

    if (isZeroSkills) {
      return {
        scorePct: 100,
        displayPct: '100%',
        badgeClass: 'badge-strong',
        fillColor: '#34d399',
        label: '100% Match (Zero Skills Required)',
        isZeroSkills: true,
      };
    }

    const rawPct = typeof match.matchPercentage === 'number' ? match.matchPercentage : 0;
    const boundedPct = Math.max(0, Math.min(100, Math.round(rawPct)));

    // Case D: Perfect match
    const isPerfect = (match.missingRequiredSkills?.length || 0) === 0 && match.totalRequiredSkills > 0 && boundedPct === 100;
    if (isPerfect) {
      return {
        scorePct: 100,
        displayPct: '100%',
        badgeClass: 'badge-strong',
        fillColor: '#34d399',
        label: '100% Match (Perfect Alignment)',
        isZeroSkills: false,
      };
    }

    // Case B: Zero user skills matched
    const isZeroUserSkills = (match.matchedRequiredSkills?.length || 0) === 0 && boundedPct === 0;
    if (isZeroUserSkills) {
      return {
        scorePct: 0,
        displayPct: '0%',
        badgeClass: 'badge-low',
        fillColor: '#f87171',
        label: '0% Match',
        isZeroSkills: false,
      };
    }

    // Case C: Partial match
    let badgeClass = 'badge-low';
    let fillColor = '#f87171';
    if (boundedPct >= 70) {
      badgeClass = 'badge-strong';
      fillColor = '#34d399';
    } else if (boundedPct >= 40) {
      badgeClass = 'badge-partial';
      fillColor = '#fbbf24';
    }

    return {
      scorePct: boundedPct,
      displayPct: `${boundedPct}%`,
      badgeClass,
      fillColor,
      label: match.matchLabel ? `${boundedPct}% Match (${match.matchLabel})` : `${boundedPct}% Match`,
      isZeroSkills: false,
    };
  };

  const presentation = getSkillMatchPresentation(skillMatch);

  const formatStipend = () => {
    if (!opportunity.stipendAmount) return 'Unspecified';
    const sym = opportunity.stipendCurrency === 'USD' ? '$' : (opportunity.stipendCurrency || '') + ' ';
    const amt = Number(opportunity.stipendAmount).toLocaleString();
    let suffix = '';
    if (opportunity.stipendPeriod === 'HOURLY') suffix = ' / hr';
    else if (opportunity.stipendPeriod === 'MONTHLY') suffix = ' / month';
    else if (opportunity.stipendPeriod === 'TOTAL') suffix = ' total';
    return `${sym}${amt}${suffix}`;
  };

  return (
    <>
      <div className="drawer-backdrop" onClick={onClose} />
      <aside className="skill-match-drawer" role="dialog" aria-modal="true" aria-label="Opportunity Details">
        <header className="drawer-header">
          <div className="drawer-header-left">
            <div className="drawer-company-row">
              <span className="drawer-company-name">{opportunity.companyName || 'Opportunity Details'}</span>
              {opportunity.isCustom && <span className="badge-custom">Custom</span>}
            </div>
            <h2 className="drawer-title">{opportunity.title}</h2>
          </div>

          <button className="drawer-close-btn" onClick={onClose} aria-label="Close drawer">
            ✕
          </button>
        </header>

        <div className="drawer-content">
          {/* Pipeline Conversion Banner */}
          <div className="drawer-pipeline-banner">
            <div className="drawer-pipeline-info">
              <span className="drawer-pipeline-label">Pipeline Status</span>
              <span className="drawer-pipeline-status">
                {pipelineApplication ? `Tracked as ${pipelineApplication.status}` : 'Not in your pipeline'}
              </span>
            </div>

            {pipelineApplication ? (
              <span className="pipeline-status-badge">✓ Active</span>
            ) : (
              <Button
                variant="primary"
                size="sm"
                onClick={() => onConvert(opportunity)}
                isLoading={isConverting}
              >
                + Convert to Application
              </Button>
            )}
          </div>

          {/* Deterministic Skill Match Engine Section */}
          <section className="skill-match-section">
            <div className="skill-match-header">
              <span className="drawer-section-title">Deterministic Skill Alignment</span>
              {skillMatch && (
                <span className={`skill-match-score-badge ${presentation.badgeClass}`}>
                  {presentation.label}
                </span>
              )}
            </div>

            {isLoadingMatch ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                <Skeleton width="100%" height="28px" />
                <Skeleton width="80%" height="20px" />
              </div>
            ) : matchError ? (
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '8px', padding: '8px 0' }}>
                <span style={{ fontSize: 'var(--font-size-xs)', color: '#f87171' }}>{matchError}</span>
                <Button variant="secondary" size="sm" onClick={fetchSkillMatch}>
                  Retry
                </Button>
              </div>
            ) : skillMatch ? (
              <>
                <div className="skill-match-score-row">
                  <span className="skill-match-large-pct" style={{ color: presentation.fillColor }}>
                    {presentation.displayPct}
                  </span>
                  <div style={{ flex: 1 }}>
                    <div className="skill-progress-bar-bg">
                      <div
                        className="skill-progress-bar-fill"
                        style={{ width: `${presentation.scorePct}%`, backgroundColor: presentation.fillColor }}
                      />
                    </div>
                    <span style={{ fontSize: '11px', color: 'var(--color-text-tertiary)', marginTop: '4px', display: 'block' }}>
                      {presentation.isZeroSkills
                        ? 'No specific technical requirements requested by posting'
                        : `${skillMatch.matchedRequiredSkills?.length || 0} of ${skillMatch.totalRequiredSkills || 0} required skills verified`}
                    </span>
                  </div>
                </div>

                {/* Required Skills */}
                <div className="skill-group">
                  <div className="skill-group-title">
                    <span>Required Tech Stack</span>
                    <span>{skillMatch.totalRequiredSkills} specified</span>
                  </div>
                  <div className="skill-chips-container">
                    {skillMatch.matchedRequiredSkills?.map((skill) => (
                      <span key={skill} className="skill-chip matched">
                        ✓ {skill}
                      </span>
                    ))}
                    {skillMatch.missingRequiredSkills?.map((skill) => (
                      <span key={skill} className="skill-chip missing">
                        ⚠ {skill}
                        <button
                          className="chip-add-btn"
                          disabled={addingSkillName === skill}
                          onClick={() => handleAddSkillToProfile(skill)}
                          title="Add to my verified skills profile"
                        >
                          {addingSkillName === skill ? '...' : '+ Add'}
                        </button>
                      </span>
                    ))}
                    {(!skillMatch.matchedRequiredSkills?.length && !skillMatch.missingRequiredSkills?.length) && (
                      <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-tertiary)' }}>
                        No required skills listed
                      </span>
                    )}
                  </div>
                </div>

                {/* Preferred Skills */}
                {(skillMatch.totalPreferredSkills > 0 || skillMatch.matchedPreferredSkills?.length > 0 || skillMatch.missingPreferredSkills?.length > 0) && (
                  <div className="skill-group">
                    <div className="skill-group-title">
                      <span>Preferred / Bonus Skills</span>
                      <span>{skillMatch.totalPreferredSkills} specified</span>
                    </div>
                    <div className="skill-chips-container">
                      {skillMatch.matchedPreferredSkills?.map((skill) => (
                        <span key={skill} className="skill-chip matched" style={{ opacity: 0.85 }}>
                          ✓ {skill}
                        </span>
                      ))}
                      {skillMatch.missingPreferredSkills?.map((skill) => (
                        <span key={skill} className="skill-chip missing" style={{ opacity: 0.85 }}>
                          ○ {skill}
                          <button
                            className="chip-add-btn"
                            disabled={addingSkillName === skill}
                            onClick={() => handleAddSkillToProfile(skill)}
                            title="Add to my verified skills profile"
                          >
                            {addingSkillName === skill ? '...' : '+ Add'}
                          </button>
                        </span>
                      ))}
                    </div>
                  </div>
                )}
              </>
            ) : null}
          </section>

          {/* Opportunity Specifications Grid */}
          <section className="drawer-details-section">
            <span className="drawer-section-title">Role Specifications</span>
            <div className="drawer-specs-grid">
              <div className="spec-item">
                <span className="spec-label">Location / Mode</span>
                <span className="spec-value">
                  {opportunity.locationType || 'Not specified'}
                  {opportunity.locationCity ? ` (${opportunity.locationCity})` : ''}
                </span>
              </div>
              <div className="spec-item">
                <span className="spec-label">Compensation</span>
                <span className="spec-value">{formatStipend()}</span>
              </div>
              <div className="spec-item">
                <span className="spec-label">Duration</span>
                <span className="spec-value">
                  {opportunity.durationMonths ? `${opportunity.durationMonths} months` : 'Flexible'}
                </span>
              </div>
              <div className="spec-item">
                <span className="spec-label">Application Deadline</span>
                <span className="spec-value">
                  {opportunity.applicationDeadline
                    ? new Date(opportunity.applicationDeadline).toLocaleDateString()
                    : 'Open until filled'}
                </span>
              </div>
            </div>
          </section>

          {/* Description */}
          {opportunity.description && (
            <section className="drawer-details-section">
              <span className="drawer-section-title">Job Description & Details</span>
              <div className="drawer-description-box">{opportunity.description}</div>
            </section>
          )}
        </div>

        <footer className="drawer-footer">
          {opportunity.sourceUrl ? (
            <a
              href={opportunity.sourceUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="meta-chip"
              style={{ textDecoration: 'none', color: 'var(--color-accent-primary)' }}
            >
              Company Posting ↗
            </a>
          ) : (
            <span style={{ fontSize: '11px', color: 'var(--color-text-tertiary)' }}>Manual Opportunity</span>
          )}

          <Button variant="secondary" size="sm" onClick={onClose}>
            Close Drawer
          </Button>
        </footer>
      </aside>
    </>
  );
};
