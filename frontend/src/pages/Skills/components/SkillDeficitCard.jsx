import React from 'react';

export const SkillDeficitCard = ({ deficit, onAddSkill, isAdding = false }) => {
  const pct = Math.min(100, Math.max(0, Math.round(deficit.frequencyPercentage || 0)));

  return (
    <div
      style={{
        background: 'var(--color-surface)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-lg)',
        padding: 'var(--space-3) var(--space-4)',
        display: 'flex',
        flexDirection: 'column',
        gap: 'var(--space-2)',
        transition: 'border-color var(--duration-fast) var(--ease-default)',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 'var(--space-2)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
          <span style={{ fontSize: 'var(--font-size-sm)', fontWeight: 'var(--font-weight-semibold)', color: 'var(--color-text-primary)' }}>
            {deficit.skillName}
          </span>
          <span
            style={{
              fontSize: '10px',
              padding: '1px 6px',
              borderRadius: 'var(--radius-full)',
              background: 'var(--color-surface-hover)',
              color: 'var(--color-text-tertiary)',
              border: '1px solid var(--color-border-subtle)',
            }}
          >
            {deficit.category}
          </span>
        </div>

        <button
          type="button"
          onClick={() => onAddSkill(deficit)}
          disabled={isAdding}
          style={{
            background: 'rgba(99, 102, 241, 0.1)',
            border: '1px solid rgba(99, 102, 241, 0.3)',
            color: 'var(--color-accent-primary)',
            borderRadius: 'var(--radius-md)',
            padding: '3px 9px',
            fontSize: '11px',
            fontWeight: 'var(--font-weight-medium)',
            cursor: 'pointer',
            display: 'inline-flex',
            alignItems: 'center',
            gap: '4px',
            transition: 'all var(--duration-fast) var(--ease-default)',
          }}
          title={`Add ${deficit.skillName} to your candidate skill profile`}
        >
          <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          Add to Profile
        </button>
      </div>

      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '11px', color: 'var(--color-text-secondary)', marginBottom: '4px' }}>
          <span>Missing in {deficit.opportunityCount} {deficit.opportunityCount === 1 ? 'opportunity' : 'opportunities'}</span>
          <span style={{ fontWeight: 'var(--font-weight-semibold)', color: 'var(--color-text-primary)' }}>{pct}%</span>
        </div>
        <div
          style={{
            height: '5px',
            borderRadius: 'var(--radius-full)',
            background: 'var(--color-surface-hover)',
            overflow: 'hidden',
          }}
        >
          <div
            style={{
              width: `${pct}%`,
              height: '100%',
              borderRadius: 'var(--radius-full)',
              background: pct > 60 ? '#ef4444' : pct > 30 ? '#f59e0b' : 'var(--color-accent-primary)',
              transition: 'width 0.4s ease-out',
            }}
          />
        </div>
      </div>
    </div>
  );
};
