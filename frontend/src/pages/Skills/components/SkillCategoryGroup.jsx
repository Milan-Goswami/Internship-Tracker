import React from 'react';
import './SkillCategoryGroup.css';

const CATEGORY_LABELS = {
  LANGUAGE: 'Programming Languages',
  FRAMEWORK: 'Frameworks & Libraries',
  DATABASE: 'Databases & Storage',
  TOOL: 'Developer Tools & Platforms',
  CLOUD: 'Cloud & Infrastructure',
  METHODOLOGY: 'Methodologies & Architecture',
};

const PROFICIENCY_CONFIG = {
  BEGINNER: { label: 'Beginner', className: 'prof-beginner' },
  INTERMEDIATE: { label: 'Intermediate', className: 'prof-intermediate' },
  ADVANCED: { label: 'Advanced', className: 'prof-advanced' },
  EXPERT: { label: 'Expert', className: 'prof-expert' },
};

export const SkillCategoryGroup = ({ category, skills = [], onRemoveSkill }) => {
  if (skills.length === 0) return null;

  const categoryLabel = CATEGORY_LABELS[category] || category;

  return (
    <div className="skill-category-group">
      <div className="skill-category-header">
        <h3 className="skill-category-title">{categoryLabel}</h3>
        <span className="skill-category-count">{skills.length}</span>
      </div>

      <div className="skill-chips-grid">
        {skills.map((s) => {
          const profConfig = PROFICIENCY_CONFIG[s.proficiencyLevel] || PROFICIENCY_CONFIG.INTERMEDIATE;

          return (
            <div key={s.id || s.skillId} className="skill-chip">
              <div className="skill-chip-main">
                <span className="skill-chip-name">{s.skillName}</span>
                <span className={`skill-chip-prof ${profConfig.className}`}>
                  {profConfig.label}
                </span>
                {s.yearsExperience !== null && s.yearsExperience !== undefined && (
                  <span className="skill-chip-exp">
                    {s.yearsExperience} {s.yearsExperience === 1 ? 'yr' : 'yrs'}
                  </span>
                )}
              </div>

              <button
                type="button"
                className="skill-chip-remove-btn"
                onClick={() => onRemoveSkill(s)}
                title={`Remove ${s.skillName} from profile`}
                aria-label={`Remove ${s.skillName}`}
              >
                &times;
              </button>
            </div>
          );
        })}
      </div>
    </div>
  );
};
