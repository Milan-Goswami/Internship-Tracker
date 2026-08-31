import React from 'react';

export default function App() {
  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      padding: 'var(--space-6)',
      textAlign: 'center'
    }}>
      <header style={{
        maxWidth: '640px',
        padding: 'var(--space-8)',
        background: 'var(--color-surface-base)',
        border: '1px solid var(--color-border-default)',
        borderRadius: 'var(--radius-md)'
      }}>
        <div style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: 'var(--space-2)',
          padding: 'var(--space-1) var(--space-3)',
          borderRadius: 'var(--radius-sm)',
          background: 'var(--color-accent-subtle)',
          color: 'var(--color-accent-primary)',
          fontSize: 'var(--font-size-xs)',
          fontWeight: 'var(--font-weight-semibold)',
          textTransform: 'uppercase',
          marginBottom: 'var(--space-4)'
        }}>
          Phase 1 Foundation Initialized
        </div>
        <h1 style={{
          fontSize: 'var(--font-size-2xl)',
          fontWeight: 'var(--font-weight-bold)',
          marginBottom: 'var(--space-2)',
          letterSpacing: '-0.01em'
        }}>
          InternTrack
        </h1>
        <p style={{
          color: 'var(--color-text-secondary)',
          fontSize: 'var(--font-size-sm)',
          lineHeight: '1.6',
          marginBottom: 'var(--space-6)'
        }}>
          Internship Application Intelligence Platform. Workspace foundation and design system tokens established.
        </p>
        <div style={{
          display: 'flex',
          justifyContent: 'center',
          gap: 'var(--space-4)',
          fontSize: 'var(--font-size-xs)',
          color: 'var(--color-text-muted)'
        }}>
          <span>Backend: Spring Boot 3.3.5 / Java 21</span>
          <span>•</span>
          <span>Frontend: React 18 / Vite</span>
        </div>
      </header>
    </div>
  );
}