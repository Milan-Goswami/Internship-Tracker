import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { CommandBar } from '../components/navigation/CommandBar';
import { AuthContext } from '../context/AuthContext';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const renderCommandBar = (authOverrides = {}) => {
  const defaultAuth = {
    user: { fullName: 'Alice Candidate', email: 'alice@example.com' },
    isAuthenticated: true,
    logout: vi.fn(),
    ...authOverrides,
  };

  return render(
    <BrowserRouter>
      <AuthContext.Provider value={defaultAuth}>
        <CommandBar onNewApplicationClick={vi.fn()} />
      </AuthContext.Provider>
    </BrowserRouter>
  );
};

describe('CommandBar Keyboard Accessibility Suite', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('focuses the global search input when / is pressed on the page', () => {
    renderCommandBar();
    const searchInput = screen.getByLabelText(/global search/i);
    expect(document.activeElement).not.toBe(searchInput);

    // Press '/' on window/body
    fireEvent.keyDown(window, { key: '/' });

    expect(document.activeElement).toBe(searchInput);
  });

  it('does NOT hijack / when typing inside an external input element', () => {
    render(
      <div>
        <input data-testid="other-input" type="text" />
        <BrowserRouter>
          <AuthContext.Provider value={{ user: null, isAuthenticated: false, logout: vi.fn() }}>
            <CommandBar />
          </AuthContext.Provider>
        </BrowserRouter>
      </div>
    );

    const otherInput = screen.getByTestId('other-input');
    const searchInput = screen.getByLabelText(/global search/i);

    otherInput.focus();
    expect(document.activeElement).toBe(otherInput);

    // Fire '/' with target as otherInput
    fireEvent.keyDown(otherInput, { key: '/' });

    // Focus must remain on the external input, NOT hijacked to searchInput
    expect(document.activeElement).toBe(otherInput);
    expect(document.activeElement).not.toBe(searchInput);
  });

  it('does NOT hijack / when typing inside a textarea or select', () => {
    render(
      <div>
        <textarea data-testid="other-textarea" />
        <select data-testid="other-select">
          <option value="1">Option 1</option>
        </select>
        <BrowserRouter>
          <AuthContext.Provider value={{ user: null, isAuthenticated: false, logout: vi.fn() }}>
            <CommandBar />
          </AuthContext.Provider>
        </BrowserRouter>
      </div>
    );

    const textarea = screen.getByTestId('other-textarea');
    const select = screen.getByTestId('other-select');
    const searchInput = screen.getByLabelText(/global search/i);

    textarea.focus();
    fireEvent.keyDown(textarea, { key: '/' });
    expect(document.activeElement).toBe(textarea);

    select.focus();
    fireEvent.keyDown(select, { key: '/' });
    expect(document.activeElement).toBe(select);
    expect(document.activeElement).not.toBe(searchInput);
  });

  it('removes focus from the search input when Escape is pressed', () => {
    renderCommandBar();
    const searchInput = screen.getByLabelText(/global search/i);

    searchInput.focus();
    expect(document.activeElement).toBe(searchInput);

    fireEvent.keyDown(searchInput, { key: 'Escape' });
    expect(document.activeElement).not.toBe(searchInput);
  });

  it('navigates to /applications with search query when Enter is pressed', () => {
    renderCommandBar();
    const searchInput = screen.getByLabelText(/global search/i);

    fireEvent.change(searchInput, { target: { value: 'Frontend' } });
    fireEvent.keyDown(searchInput, { key: 'Enter' });

    expect(mockNavigate).toHaveBeenCalledWith('/applications?q=Frontend');
    expect(document.activeElement).not.toBe(searchInput);
  });

  it('removes global event listener when component unmounts', () => {
    const removeEventListenerSpy = vi.spyOn(window, 'removeEventListener');
    const { unmount } = renderCommandBar();

    unmount();

    expect(removeEventListenerSpy).toHaveBeenCalledWith('keydown', expect.any(Function));
    removeEventListenerSpy.mockRestore();
  });
});
