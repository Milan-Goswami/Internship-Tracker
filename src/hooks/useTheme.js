import { useState, useEffect, useCallback } from 'react';

const STORAGE_KEY = 'portfolio-theme';

export function getInitialTheme() {
  if (typeof window === 'undefined') return 'dark';
  
  // 1. Saved manual preference (persisted user choice)
  const saved = localStorage.getItem(STORAGE_KEY);
  if (saved === 'dark' || saved === 'light') {
    return saved;
  }
  
  // 2. DOM attribute if set by early bootstrap script in index.html
  const domTheme = document.documentElement.getAttribute('data-theme');
  if (domTheme === 'dark' || domTheme === 'light') {
    return domTheme;
  }

  // 3. Default Dark Mode for first-time visitors with no saved preference
  return 'dark';
}

export function useTheme() {
  const [theme, setTheme] = useState(getInitialTheme);

  const applyTheme = useCallback((newTheme) => {
    setTheme(newTheme);
    document.documentElement.setAttribute('data-theme', newTheme);
    localStorage.setItem(STORAGE_KEY, newTheme);

    // Update meta theme-color for mobile browser address bars
    const metaTheme = document.querySelector('meta[name="theme-color"]');
    if (metaTheme) {
      metaTheme.setAttribute('content', newTheme === 'dark' ? '#0C0F0C' : '#F8F9F5');
    }

    // Broadcast change for components that render canvas or need immediate redraw
    window.dispatchEvent(new CustomEvent('portfolio-theme-change', { detail: { theme: newTheme } }));
  }, []);

  const toggleTheme = useCallback(() => {
    applyTheme(theme === 'dark' ? 'light' : 'dark');
  }, [theme, applyTheme]);

  useEffect(() => {
    // Listen to changes from other tabs or custom events
    const handleThemeEvent = (e) => {
      if (e.detail?.theme) {
        setTheme(e.detail.theme);
      }
    };
    const handleStorage = (e) => {
      if (e.key === STORAGE_KEY && (e.newValue === 'dark' || e.newValue === 'light')) {
        setTheme(e.newValue);
        document.documentElement.setAttribute('data-theme', e.newValue);
      }
    };
    window.addEventListener('portfolio-theme-change', handleThemeEvent);
    window.addEventListener('storage', handleStorage);

    return () => {
      window.removeEventListener('portfolio-theme-change', handleThemeEvent);
      window.removeEventListener('storage', handleStorage);
    };
  }, []);

  return { theme, isDark: theme === 'dark', toggleTheme, setTheme: applyTheme };
}
