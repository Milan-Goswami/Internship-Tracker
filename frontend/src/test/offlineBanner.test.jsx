import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import { OfflineBanner } from '../components/common/OfflineBanner';

describe('OfflineBanner Component Suite', () => {
  let originalOnLine;

  beforeEach(() => {
    originalOnLine = navigator.onLine;
    vi.useFakeTimers();
  });

  afterEach(() => {
    Object.defineProperty(navigator, 'onLine', {
      value: originalOnLine,
      configurable: true,
      writable: true,
    });
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it('renders nothing when browser is online initially', () => {
    Object.defineProperty(navigator, 'onLine', { value: true, configurable: true });
    const { container } = render(<OfflineBanner />);
    expect(container.firstChild).toBeNull();
  });

  it('renders offline alert when browser is offline initially', () => {
    Object.defineProperty(navigator, 'onLine', { value: false, configurable: true });
    render(<OfflineBanner />);

    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(screen.getByText(/you are currently offline/i)).toBeInTheDocument();
  });

  it('displays offline banner when window receives offline event', () => {
    Object.defineProperty(navigator, 'onLine', { value: true, configurable: true });
    render(<OfflineBanner />);

    expect(screen.queryByRole('status')).toBeNull();

    // Trigger offline event
    act(() => {
      window.dispatchEvent(new Event('offline'));
    });

    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(screen.getByText(/you are currently offline/i)).toBeInTheDocument();
  });

  it('displays transient reconnection message when online event fires', () => {
    Object.defineProperty(navigator, 'onLine', { value: false, configurable: true });
    render(<OfflineBanner />);

    expect(screen.getByText(/you are currently offline/i)).toBeInTheDocument();

    // Trigger online event
    act(() => {
      window.dispatchEvent(new Event('online'));
    });

    // Reconnection banner must be shown
    expect(screen.getByText(/connection restored/i)).toBeInTheDocument();

    // Fast-forward past the 3500ms auto-dismiss timeout
    act(() => {
      vi.advanceTimersByTime(3600);
    });

    // Banner must now be hidden
    expect(screen.queryByRole('status')).toBeNull();
  });

  it('removes window event listeners on unmount', () => {
    const removeEventListenerSpy = vi.spyOn(window, 'removeEventListener');
    const { unmount } = render(<OfflineBanner />);

    unmount();

    expect(removeEventListenerSpy).toHaveBeenCalledWith('online', expect.any(Function));
    expect(removeEventListenerSpy).toHaveBeenCalledWith('offline', expect.any(Function));
  });

  it('handles remounting without leaking duplicate events', () => {
    const { unmount } = render(<OfflineBanner />);
    unmount();

    // Remount
    render(<OfflineBanner />);

    act(() => {
      window.dispatchEvent(new Event('offline'));
    });

    expect(screen.getAllByRole('status')).toHaveLength(1);
  });
});
