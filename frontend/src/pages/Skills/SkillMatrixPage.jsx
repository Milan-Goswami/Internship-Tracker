import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { skillService } from '../../services/skillService';
import { analyticsService } from '../../services/analyticsService';
import { SkillCategoryGroup } from './components/SkillCategoryGroup';
import { AddSkillModal } from './components/AddSkillModal';
import { SkillDeficitCard } from './components/SkillDeficitCard';
import { EmptyState } from '../../components/states/EmptyState';
import { ErrorState } from '../../components/states/ErrorState';
import { Skeleton } from '../../components/common/Skeleton';
import { Button } from '../../components/common/Button';
import { useToast } from '../../context/ToastContext';
import './SkillMatrixPage.css';

const CATEGORIES = [
  { id: 'ALL', label: 'All Categories' },
  { id: 'LANGUAGE', label: 'Languages' },
  { id: 'FRAMEWORK', label: 'Frameworks' },
  { id: 'DATABASE', label: 'Databases' },
  { id: 'TOOL', label: 'Tools' },
  { id: 'CLOUD', label: 'Cloud' },
  { id: 'METHODOLOGY', label: 'Methodologies' },
];

export const SkillMatrixAndTaxonomy = () => {
  const [userSkills, setUserSkills] = useState([]);
  const [deficits, setDeficits] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const [activeCategory, setActiveCategory] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [preSelectedSkill, setPreSelectedSkill] = useState(null);

  const { showToast } = useToast();

  const fetchData = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [skillsData, deficitsData] = await Promise.all([
        skillService.getUserSkills(),
        analyticsService.getSkillDeficits(),
      ]);
      setUserSkills(Array.isArray(skillsData) ? skillsData : []);
      setDeficits(deficitsData || null);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to load skill matrix');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // Group user skills by category and apply client-side search/category filters
  const filteredSkills = useMemo(() => {
    return userSkills.filter((s) => {
      if (activeCategory !== 'ALL' && s.category !== activeCategory) {
        return false;
      }
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matchesName = (s.skillName || '').toLowerCase().includes(q);
        const matchesCat = (s.category || '').toLowerCase().includes(q);
        if (!matchesName && !matchesCat) return false;
      }
      return true;
    });
  }, [userSkills, activeCategory, searchQuery]);

  const skillsByCategory = useMemo(() => {
    const groups = {};
    filteredSkills.forEach((s) => {
      const cat = s.category || 'TOOL';
      if (!groups[cat]) groups[cat] = [];
      groups[cat].push(s);
    });
    return groups;
  }, [filteredSkills]);

  // Add skill to profile
  const handleAddSkill = async (payload, rawSkill) => {
    const created = await skillService.addUserSkill(payload);
    setUserSkills((prev) => [...prev, created]);
    showToast(`Added ${rawSkill.name || rawSkill.skillName} to profile`, 'success');

    // Authoritative backend re-fetch of skill deficits
    try {
      const refreshedDeficits = await analyticsService.getSkillDeficits();
      setDeficits(refreshedDeficits);
    } catch {
      // Fallback local update if network error
      if (deficits && deficits.topDeficits) {
        setDeficits((prev) => ({
          ...prev,
          candidateSkillCount: (prev.candidateSkillCount || 0) + 1,
          topDeficits: prev.topDeficits.filter(
            (d) => d.skillName?.toLowerCase() !== (rawSkill.name || rawSkill.skillName)?.toLowerCase()
          ),
        }));
      }
    }
  };

  // Quick-add from deficit card (opens modal pre-filled with validated skillId)
  const handleDeficitQuickAdd = (deficit) => {
    setPreSelectedSkill({
      id: deficit.skillId || null,
      name: deficit.skillName,
      skillName: deficit.skillName,
      category: deficit.category,
    });
    setIsAddModalOpen(true);
  };

  // Remove skill with rollback on error and authoritative deficit refresh
  const handleRemoveSkill = async (userSkill) => {
    const prevSkills = userSkills;
    const skillId = userSkill.skillId || userSkill.id;

    // Optimistic local removal
    setUserSkills((prev) => prev.filter((s) => (s.skillId || s.id) !== skillId));

    try {
      await skillService.removeUserSkill(skillId);
      showToast(`Removed ${userSkill.skillName} from profile`, 'info');
      // Authoritative backend re-fetch of skill deficits
      const refreshedDeficits = await analyticsService.getSkillDeficits();
      setDeficits(refreshedDeficits);
    } catch (err) {
      // Rollback on failure
      setUserSkills(prevSkills);
      showToast(err.response?.data?.message || 'Failed to remove skill', 'error');
    }
  };

  return (
    <div className="skills-page">
      <header className="skills-page-header">
        <div>
          <h1 className="skills-page-title">Skill Matrix & Taxonomy</h1>
          <p className="skills-page-subtitle">
            Manage verified candidate skills and view cross-opportunity deficit intelligence.
          </p>
        </div>
        <Button
          variant="primary"
          onClick={() => {
            setPreSelectedSkill(null);
            setIsAddModalOpen(true);
          }}
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          Add Skill
        </Button>
      </header>

      {isLoading ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
          <Skeleton width="100%" height="80px" style={{ borderRadius: '12px' }} />
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: 'var(--space-4)' }}>
            <Skeleton width="100%" height="160px" style={{ borderRadius: '12px' }} />
            <Skeleton width="100%" height="160px" style={{ borderRadius: '12px' }} />
          </div>
        </div>
      ) : error ? (
        <ErrorState
          title="Failed to Load Skill Matrix"
          description={error}
          onRetry={fetchData}
        />
      ) : (
        <>
          {/* SECTION 1: Candidate Skills Inventory */}
          <section className="skills-section">
            <div className="skills-toolbar">
              <div className="skills-tabs" role="tablist">
                {CATEGORIES.map((cat) => (
                  <button
                    key={cat.id}
                    role="tab"
                    aria-selected={activeCategory === cat.id}
                    className={`skills-tab-btn ${activeCategory === cat.id ? 'active' : ''}`}
                    onClick={() => setActiveCategory(cat.id)}
                  >
                    {cat.label}
                    <span className="skills-tab-count">
                      {cat.id === 'ALL'
                        ? userSkills.length
                        : userSkills.filter((s) => s.category === cat.id).length}
                    </span>
                  </button>
                ))}
              </div>

              <div className="skills-search-box">
                <svg className="skills-search-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="11" cy="11" r="8" />
                  <line x1="21" y1="21" x2="16.65" y2="16.65" />
                </svg>
                <input
                  type="text"
                  className="skills-search-input"
                  placeholder="Search candidate skills..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
                {searchQuery && (
                  <button
                    type="button"
                    className="skills-search-clear"
                    onClick={() => setSearchQuery('')}
                    aria-label="Clear search"
                  >
                    &times;
                  </button>
                )}
              </div>
            </div>

            {userSkills.length === 0 ? (
              <EmptyState
                title="No Skills in Profile Yet"
                description="Build your verified technical inventory. The deterministic engine matches your skills against saved opportunities in real-time."
                actionLabel="+ Add Your First Skill"
                onAction={() => {
                  setPreSelectedSkill(null);
                  setIsAddModalOpen(true);
                }}
              />
            ) : filteredSkills.length === 0 ? (
              <EmptyState
                title="No Matching Skills"
                description="No candidate skills match your search criteria. Try a different term or clear category filters."
                actionLabel="Reset Filters"
                onAction={() => {
                  setActiveCategory('ALL');
                  setSearchQuery('');
                }}
              />
            ) : (
              <div className="skills-category-groups-grid">
                {Object.keys(skillsByCategory).map((cat) => (
                  <SkillCategoryGroup
                    key={cat}
                    category={cat}
                    skills={skillsByCategory[cat]}
                    onRemoveSkill={handleRemoveSkill}
                  />
                ))}
              </div>
            )}
          </section>

          {/* SECTION 2: Cross-Opportunity Deficit Intelligence */}
          {deficits && deficits.topDeficits && deficits.topDeficits.length > 0 && (
            <section className="skills-deficit-section">
              <div className="deficit-header-row">
                <div>
                  <h2 className="deficit-section-title">
                    Cross-Opportunity Skill Deficit Intelligence
                  </h2>
                  <p className="deficit-section-subtitle">
                    Top missing skills identified across your {deficits.totalTrackedOpportunities || 0} tracked opportunities. Add them to your profile as you learn them.
                  </p>
                </div>
                <div className="deficit-summary-pill">
                  <span>{deficits.topDeficits.length} Skill Deficits Detected</span>
                </div>
              </div>

              <div className="deficit-cards-grid">
                {deficits.topDeficits.map((d, idx) => (
                  <SkillDeficitCard
                    key={`${d.canonicalName}-${idx}`}
                    deficit={d}
                    onAddSkill={handleDeficitQuickAdd}
                  />
                ))}
              </div>
            </section>
          )}
        </>
      )}

      {/* Add Skill Modal */}
      <AddSkillModal
        isOpen={isAddModalOpen}
        onClose={() => {
          setIsAddModalOpen(false);
          setPreSelectedSkill(null);
        }}
        onAddSkill={handleAddSkill}
        preSelectedSkill={preSelectedSkill}
      />
    </div>
  );
};

export const SkillMatrixPage = SkillMatrixAndTaxonomy;
export default SkillMatrixAndTaxonomy;
