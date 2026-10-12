import React from 'react';
import { Outlet } from 'react-router-dom';
import { Sidebar } from '../components/navigation/Sidebar';
import { CommandBar } from '../components/navigation/CommandBar';
import { OfflineBanner } from '../components/common/OfflineBanner';
import './AppLayout.css';

export const AppLayout = () => {
  return (
    <div className="app-layout">
      <Sidebar />
      <div className="app-main-content">
        <OfflineBanner />
        <CommandBar />
        <main className="app-viewport">
          <Outlet />
        </main>
      </div>
    </div>
  );
};