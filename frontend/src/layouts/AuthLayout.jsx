import React from 'react';
import { Outlet } from 'react-router-dom';
import './AuthLayout.css';

export const AuthLayout = () => {
  return (
    <div className="auth-layout">
      <div className="auth-card">
        <div className="auth-header">
          <div className="auth-logo">IT</div>
          <h1 className="auth-title">InternTrack</h1>
          <p className="auth-subtitle">Application Intelligence & Preparation Platform</p>
        </div>
        <Outlet />
      </div>
    </div>
  );
};