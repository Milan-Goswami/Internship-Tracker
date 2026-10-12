import React, { useState, useEffect } from 'react';
import { skillService } from '../../../services/skillService';
import { Button } from '../../../components/common/Button';
import './AddSkillModal.css';

const PROFICIENCY_LEVELS = [
  { value: 'BEGINNER', label: 'Beginner' },
  { value: 'INTERMEDIATE', label: 'Intermediate' },
  { value: 'ADVANCED', label: 'Advanced' },
  { value: 'EXPERT', label: 'Expert' },
];

export const AddSkillModal = ({
  isOpen,
  onClose,
  onAddSkill,
  preSelectedSkill = null,
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [isSearching, setIsSearching] = useState(false);

  const [selectedSkill, setSelectedSkill] = useState(null);
  const [proficiencyLevel, setProficiencyLevel] = useState('INTERMEDIATE');
  const [yearsExperience, setYearsExperience] = useState(1);

  const [error, setError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (preSelectedSkill) {
      if (!preSelectedSkill.id && (preSelectedSkill.name || preSelectedSkill.skillName)) {
        const query = preSelectedSkill.name || preSelectedSkill.skillName;
        skillService
          .searchSkills(query)
          .then((results) => {
            const match = Array.isArray(results)
              ? results.find(
                  (r) =>
                    r.canonicalName === preSelectedSkill.canonicalName ||
                    r.name?.toLowerCase() === query.toLowerCase()
                ) || results[0]
              : null;
            if (match) {
              setSelectedSkill(match);
            } else {
              setSelectedSkill(preSelectedSkill);
            }
          })
          .catch(() => {
            setSelectedSkill(preSelectedSkill);
          });
      } else {
        setSelectedSkill(preSelectedSkill);
      }
      setSearchQuery('');
      setSearchResults([]);
    } else {
      setSelectedSkill(null);
      setSearchQuery('');
      setSearchResults([]);
    }
    setProficiencyLevel('INTERMEDIATE');
    setYearsExperience(1);
    setError(null);
  }, [preSelectedSkill, isOpen]);

  // Search autocomplete
  useEffect(() => {
    if (!isOpen || selectedSkill) return;

    const timer = setTimeout(async () => {
      setIsSearching(true);
      try {
        const results = await skillService.searchSkills(searchQuery.trim());
        setSearchResults(Array.isArray(results) ? results : []);
      } catch (err) {
        console.error('Skill search error:', err);
      } finally {
        setIsSearching(false);
      }
    }, 250);

    return () => clearTimeout(timer);
  }, [searchQuery, isOpen, selectedSkill]);

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!selectedSkill) {
      setError('Please select a skill from the search results.');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      let targetSkillId = selectedSkill.id;

      // Fallback resolution if skillId is not present
      if (!targetSkillId && (selectedSkill.name || selectedSkill.skillName)) {
        const query = selectedSkill.name || selectedSkill.skillName;
        const results = await skillService.searchSkills(query);
        const match = Array.isArray(results)
          ? results.find(
              (r) =>
                r.canonicalName === selectedSkill.canonicalName ||
                r.name?.toLowerCase() === query.toLowerCase()
            ) || results[0]
          : null;
        if (match) {
          targetSkillId = match.id;
        }
      }

      if (!targetSkillId) {
        setError('Could not resolve skill ID in catalog. Please search and select again.');
        setIsSubmitting(false);
        return;
      }

      const payload = {
        skillId: targetSkillId,
        proficiencyLevel,
        yearsExperience: Number(yearsExperience) || 0,
      };

      await onAddSkill(payload, selectedSkill);
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to add skill to profile');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" style={{ maxWidth: '460px' }} onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2 className="modal-title">Add Skill to Profile</h2>
          <button className="modal-close-btn" onClick={onClose} aria-label="Close modal">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="modal-body">
          <div className="add-skill-form">
            {error && (
              <div
                style={{
                  padding: '8px 12px',
                  backgroundColor: 'rgba(239, 68, 68, 0.1)',
                  border: '1px solid rgba(239, 68, 68, 0.2)',
                  borderRadius: 'var(--radius-md)',
                  color: '#ef4444',
                  fontSize: 'var(--font-size-xs)',
                }}
              >
                {error}
              </div>
            )}

            {/* Skill Selector / Autocomplete */}
            <div className="skill-form-group">
              <label className="skill-form-label">Search Canonical Skill *</label>

              {selectedSkill ? (
                <div className="selected-skill-badge-row">
                  <div className="selected-skill-pill">
                    <span className="selected-skill-name">{selectedSkill.name || selectedSkill.skillName}</span>
                    <span className="selected-skill-category">{selectedSkill.category}</span>
                  </div>
                  <button
                    type="button"
                    className="selected-skill-clear-btn"
                    onClick={() => {
                      setSelectedSkill(null);
                      setSearchQuery('');
                    }}
                  >
                    Change
                  </button>
                </div>
              ) : (
                <div style={{ position: 'relative' }}>
                  <input
                    type="text"
                    className="skill-form-input skill-search-input"
                    placeholder="Search e.g. Java, Docker, React, AWS, PostgreSQL..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    autoFocus
                  />

                  {/* Dropdown Results */}
                  <div className="skill-search-dropdown">
                    {isSearching ? (
                      <div className="skill-search-msg">Searching canonical taxonomy...</div>
                    ) : searchResults.length === 0 ? (
                      <div className="skill-search-msg">
                        {searchQuery.trim() ? `No skills matching "${searchQuery}"` : 'Type to search canonical skills...'}
                      </div>
                    ) : (
                      searchResults.slice(0, 8).map((skill) => (
                        <div
                          key={skill.id}
                          className="skill-search-item"
                          onClick={() => {
                            setSelectedSkill(skill);
                            setSearchResults([]);
                          }}
                        >
                          <span style={{ fontWeight: 'var(--font-weight-medium)' }}>{skill.name}</span>
                          <span className="skill-item-category">{skill.category}</span>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              )}
            </div>

            {/* Proficiency Level */}
            <div className="skill-form-group">
              <label className="skill-form-label">Proficiency Level *</label>
              <select
                className="skill-form-select"
                value={proficiencyLevel}
                onChange={(e) => setProficiencyLevel(e.target.value)}
                required
              >
                {PROFICIENCY_LEVELS.map((lvl) => (
                  <option key={lvl.value} value={lvl.value}>
                    {lvl.label}
                  </option>
                ))}
              </select>
            </div>

            {/* Years of Experience */}
            <div className="skill-form-group">
              <label className="skill-form-label">Years of Experience</label>
              <input
                type="number"
                min="0"
                max="50"
                className="skill-form-input"
                value={yearsExperience}
                onChange={(e) => setYearsExperience(e.target.value)}
              />
            </div>
          </div>

          <div className="modal-footer" style={{ padding: 'var(--space-4) 0 0 0', display: 'flex', justifyContent: 'flex-end', gap: 'var(--space-3)' }}>
            <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" loading={isSubmitting} disabled={!selectedSkill}>
              Add to Profile
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
