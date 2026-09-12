import React, { Suspense, lazy } from 'react';
import { useAuth } from './contexts/AuthContext';
import { Navbar } from './components/Navbar';
import { Loader2 } from 'lucide-react';

const AuthPage = lazy(() => import('./pages/AuthPage').then((m) => ({ default: m.AuthPage })));
const ClientDashboard = lazy(() => import('./pages/ClientDashboard').then((m) => ({ default: m.ClientDashboard })));
const AdminDashboard = lazy(() => import('./pages/AdminDashboard').then((m) => ({ default: m.AdminDashboard })));

const FallbackSpinner: React.FC = () => (
  <div className="min-h-[50vh] flex flex-col items-center justify-center gap-3">
    <Loader2 className="w-7 h-7 text-white/70 animate-spin" />
    <span className="text-xs text-white/50 font-medium tracking-tight">Carregando...</span>
  </div>
);

export const App: React.FC = () => {
  const { user, isAdmin } = useAuth();

  if (!user) {
    return (
      <Suspense fallback={<FallbackSpinner />}>
        <AuthPage />
      </Suspense>
    );
  }

  return (
    <div className="min-h-screen bg-black text-white flex flex-col selection:bg-white/20 selection:text-white">
      <Navbar />
      <main className="flex-1">
        <Suspense fallback={<FallbackSpinner />}>
          {isAdmin ? <AdminDashboard /> : <ClientDashboard />}
        </Suspense>
      </main>
    </div>
  );
};
