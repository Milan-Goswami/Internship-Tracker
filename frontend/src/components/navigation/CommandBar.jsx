import React, { useState, useRef, useEffect } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { Button } from '../common/Button';
import { useNavigate } from 'react-router-dom';
import './CommandBar.css';

export const CommandBar = ({ onNewApplicationClick }) => {
  const { user, logout } = useAuth();
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const dropdownRef = useRef(null);
  const searchInputRef = useRef(null);
  const navigate = useNavigate();

  // Click outside listener for user dropdown
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsDropdownOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Keyboard shortcut listener for /, Ctrl+K / Cmd+K, and Escape
  useEffect(() => {
    const handleGlobalKeyDown = (e) => {
      const target = e.target;
      const isEditable =
        target.tagName === 'INPUT' ||
        target.tagName === 'TEXTAREA' ||
        target.tagName === 'SELECT' ||
        target.isContentEditable;

      const isModalActive = Boolean(
        document.querySelector('.modal-overlay, [role="dialog"], [aria-modal="true"]')
      );

      // Do not intercept shortcuts if a modal or drawer is active
      if (isModalActive) {
        return;
      }

      const isSlash = e.key === '/' && !isEditable && !e.ctrlKey && !e.metaKey && !e.altKey;
      const isCtrlK = (e.ctrlKey || e.metaKey) && (e.key === 'k' || e.key === 'K');

      // Check if single-key shortcuts are disabled by user preference (WCAG 2.1 SC 2.1.4)
      const isSingleKeyDisabled =
        isSlash &&
        typeof window !== 'undefined' &&
        window.localStorage?.getItem('interntrack_disable_single_key_shortcuts') === 'true';

      if ((isSlash && !isSingleKeyDisabled) || isCtrlK) {
        e.preventDefault();
        searchInputRef.current?.focus();
        searchInputRef.current?.select();
      } else if (e.key === 'Escape') {
        if (isDropdownOpen) {
          setIsDropdownOpen(false);
        } else if (document.activeElement === searchInputRef.current) {
          searchInputRef.current?.blur();
        }
      }
    };

    window.addEventListener('keydown', handleGlobalKeyDown);
    return () => window.removeEventListener('keydown', handleGlobalKeyDown);
  }, [isDropdownOpen]);

  const handleSearchKeyDown = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      const q = searchQuery.trim();
      navigate(`/applications${q ? `?q=${encodeURIComponent(q)}` : ''}`);
      searchInputRef.current?.blur();
    } else if (e.key === 'Escape') {
      searchInputRef.current?.blur();
    }
  };

  const handleLogout = async () => {
    setIsDropdownOpen(false);
    await logout();
    navigate('/login');
  };

  const initials = user?.fullName
    ? user.fullName
        .split(' ')
        .map((n) => n[0])
        .join('')
        .slice(0, 2)
        .toUpperCase()
    : 'U';

  return (
    <header className="command-bar">
      <div className="command-bar-left">
        <div className="global-search-wrapper">
          <svg
            className="global-search-icon"
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
          >
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            ref={searchInputRef}
            type="text"
            className="global-search-input"
            placeholder="Search applications, skills, notes..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onKeyDown={handleSearchKeyDown}
            aria-label="Global search (Press / to focus, Esc to blur)"
          />
          <span className="search-shortcut" title="Press / or Ctrl+K to focus search">/</span>
        </div>
      </div>

      <div className="command-bar-right">
        <Button
          variant="primary"
          size="sm"
          onClick={onNewApplicationClick || (() => navigate('/applications'))}
          icon={
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
          }
        >
          New Application
        </Button>

        <div className="user-menu-wrapper" ref={dropdownRef}>
          <button
            className="user-profile-trigger"
            onClick={() => setIsDropdownOpen(!isDropdownOpen)}
            aria-expanded={isDropdownOpen}
          >
            <div className="user-avatar-circle">{initials}</div>
            <span>{user?.fullName || 'Candidate'}</span>
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <polyline points="6 9 12 15 18 9" />
            </svg>
          </button>

          {isDropdownOpen && (
            <div className="user-dropdown-menu">
              <div className="user-dropdown-header">
                <div className="user-dropdown-name">{user?.fullName || 'User'}</div>
                <div className="user-dropdown-email">{user?.email}</div>
              </div>
              <button
                className="dropdown-item"
                onClick={() => {
                  setIsDropdownOpen(false);
                  navigate('/settings');
                }}
              >
                Settings & Preferences
              </button>
              <button className="dropdown-item danger" onClick={handleLogout}>
                Sign Out
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};