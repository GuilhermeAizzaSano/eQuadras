import React from 'react';
import { useAuth } from './contexts/AuthContext';
import { AuthPage } from './pages/AuthPage';
import { Navbar } from './components/Navbar';
import { ClientDashboard } from './pages/ClientDashboard';
import { AdminDashboard } from './pages/AdminDashboard';

export const App: React.FC = () => {
  const { user, isAdmin } = useAuth();

  if (!user) {
    return <AuthPage />;
  }

  return (
    <div className="min-h-screen bg-black text-zinc-100 flex flex-col">
      <Navbar />
      <main className="flex-1">
        {isAdmin ? <AdminDashboard /> : <ClientDashboard />}
      </main>
    </div>
  );
};
