import React, { Suspense, lazy } from 'react';
import { useAuth } from './contexts/AuthContext';
import { Navbar } from './components/Navbar';
import { Loader2 } from 'lucide-react';

const AuthPage = lazy(() => import('./pages/AuthPage').then((m) => ({ default: m.AuthPage })));
const ClientDashboard = lazy(() => import('./pages/ClientDashboard').then((m) => ({ default: m.ClientDashboard })));
const AdminDashboard = lazy(() => import('./pages/AdminDashboard').then((m) => ({ default: m.AdminDashboard })));

const FallbackSpinner: React.FC = () => (
  <div className="min-h-[50vh] flex flex-col items-center justify-center gap-3">
    <Loader2 className="w-8 h-8 text-emerald-500 animate-spin" />
    <span className="text-xs text-zinc-400 font-medium">Carregando...</span>
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
    <div className="min-h-screen bg-zinc-950 text-zinc-100 flex flex-col selection:bg-emerald-500/20 selection:text-emerald-300">
      <Navbar />
      <main className="flex-1">
        <Suspense fallback={<FallbackSpinner />}>
          {isAdmin ? <AdminDashboard /> : <ClientDashboard />}
        </Suspense>
      </main>
    </div>
  );
};
