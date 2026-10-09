import { Sun, Moon } from 'lucide-react';
import { useTheme } from '../hooks/useTheme';
import './ThemeToggle.css';

export default function ThemeToggle({ className = '', id = 'theme-toggle-control' }) {
  const { theme, isDark, toggleTheme } = useTheme();

  return (
    <button
      id={id}
      type="button"
      className={`theme-toggle-btn ${isDark ? 'is-dark' : 'is-light'} ${className}`}
      onClick={toggleTheme}
      aria-label={isDark ? "Switch to light theme" : "Switch to dark theme"}
      aria-pressed={isDark}
      title={isDark ? "Switch to light theme (L)" : "Switch to dark theme (D)"}
    >
      <div className="theme-toggle-icon-wrap" aria-hidden="true">
        {/* Sun Icon for Light Mode */}
        <span className={`theme-icon sun-icon ${!isDark ? 'active' : ''}`}>
          <Sun size={19} strokeWidth={2.1} />
        </span>
        {/* Moon Icon for Dark Mode */}
        <span className={`theme-icon moon-icon ${isDark ? 'active' : ''}`}>
          <Moon size={18} strokeWidth={2.1} />
        </span>
      </div>
      <span className="theme-sr-label sr-only">
        Current theme: {theme}. Click to switch theme.
      </span>
    </button>
  );
}
