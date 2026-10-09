import { techMeta } from '../data/techMeta';

export function TechBadge({ name, showIcon = true, size = 'md', className = '' }) {
  const meta = techMeta[name] || {
    color: 'var(--text-main)',
    bg: 'rgba(0,0,0,0.04)',
    border: 'rgba(0,0,0,0.1)',
    icon: null
  };

  return (
    <span 
      className={`tech-badge-pill tech-badge-${size} ${className}`}
      style={{
        '--badge-color': meta.color,
        '--badge-bg': meta.bg,
        '--badge-border': meta.border
      }}
      title={name}
    >
      {showIcon && meta.icon && (
        <span className="tech-badge-icon" aria-hidden="true">
          {meta.icon}
        </span>
      )}
      <span className="tech-badge-text">{name}</span>
    </span>
  );
}
