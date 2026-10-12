import React, { useState, useEffect } from 'react';
import './OfflineBanner.css';

export const OfflineBanner = () => {
  const [isOnline, setIsOnline] = useState(() =>
    typeof navigator !== 'undefined' ? navigator.onLine : true
  );
  const [showReconnected, setShowReconnected] = useState(false);

  useEffect(() => {
    let timer = null;

    const handleOnline = () => {
      setIsOnline(true);
      setShowReconnected(true);
      timer = setTimeout(() => {
        setShowReconnected(false);
      }, 3500);
    };

    const handleOffline = () => {
      setIsOnline(false);
      setShowReconnected(false);
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
      if (timer) clearTimeout(timer);
    };
  }, []);

  if (isOnline && !showReconnected) {
    return null;
  }

  return (
    <div
      className={`offline-banner ${isOnline ? 'reconnected' : 'offline'}`}
      role="status"
      aria-live="polite"
    >
      <span className="offline-status-dot" />
      <span>
        {isOnline
          ? 'Connection restored. Reconnected to server.'
          : 'You are currently offline. Changes will not be saved until reconnection.'}
      </span>
    </div>
  );
};
