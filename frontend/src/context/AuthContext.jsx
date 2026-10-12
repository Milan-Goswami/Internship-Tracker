import React, { createContext, useState, useEffect, useCallback } from 'react';
import { authService } from '../services/authService';
import { setAuthToken, setOnSessionExpired } from '../services/api';
import { useToast } from './ToastContext';

export const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [accessToken, setAccessToken] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const { showToast } = useToast();

  const handleLogout = useCallback(async () => {
    try {
      await authService.logout();
    } catch {
      // Ignore network errors on logout
    } finally {
      setUser(null);
      setAccessToken(null);
      setAuthToken(null);
    }
  }, []);

  useEffect(() => {
    setOnSessionExpired(() => {
      setUser(null);
      setAccessToken(null);
      setAuthToken(null);
      showToast('Session expired. Please log in again.', 'info');
    });
  }, [showToast]);

  // Check initial session via silent refresh on mount
  useEffect(() => {
    let isMounted = true;

    const checkSession = async () => {
      try {
        const { user: refreshedUser, accessToken: newToken } = await authService.refresh();
        if (isMounted) {
          setUser(refreshedUser);
          setAccessToken(newToken);
          setAuthToken(newToken);
        }
      } catch {
        // No active refresh token or invalid token; unauthenticated visitor
        if (isMounted) {
          setUser(null);
          setAccessToken(null);
          setAuthToken(null);
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    };

    checkSession();

    return () => {
      isMounted = false;
    };
  }, []);

  const login = async (credentials) => {
    const { user: loggedInUser, accessToken: token } = await authService.login(credentials);
    setUser(loggedInUser);
    setAccessToken(token);
    setAuthToken(token);
    return loggedInUser;
  };

  const register = async (userData) => {
    const response = await authService.register(userData);
    return response;
  };

  const updateUser = (updatedUser) => {
    setUser((prev) => ({ ...prev, ...updatedUser }));
  };

  const value = {
    user,
    accessToken,
    isAuthenticated: Boolean(user && accessToken),
    isLoading,
    login,
    register,
    logout: handleLogout,
    updateUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};