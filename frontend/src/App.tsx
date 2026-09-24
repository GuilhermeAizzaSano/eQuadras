import React, { Suspense, lazy, useState } from 'react';
import { useAuth } from './contexts/AuthContext';
import { DashboardLayout } from './components/layout/DashboardLayout';
import { NavigationTab, ClientTab, AdminTab } from './components/layout/Sidebar';
import { Loader2 } from 'lucide-react';

const AuthPage = lazy(() => import('./pages/AuthPage').then((m) => ({ default: m.AuthPage })));
const ClientDashboard = lazy(() => import('./pages/ClientDashboard').then((m) => ({ default: m.ClientDashboard })));
const AdminDashboard = lazy(() => import('./pages/AdminDashboard').then((m) => ({ default: m.AdminDashboard })));

const FallbackSpinner: React.FC = () => (
  <div className="min-h-[50vh] flex flex-col items-center justify-center gap-3">
    <Loader2 className="w-7 h-7 text-fg/70 animate-spin" />
    <span className="text-xs text-fg/50 font-medium tracking-tight">Carregando...</span>
  </div>
);

export const App: React.FC = () => {
  const { user, isAdmin, loadingAuth } = useAuth();
  
  // Abas do Cliente: 'QUADRAS' | 'AGENDAS'
  // Abas do Admin: 'RELATORIOS' | 'GESTAO_QUADRAS' | 'USUARIOS' | 'AUDITORIA'
  const [clientTab, setClientTab] = useState<'QUADRAS' | 'AGENDAS'>('QUADRAS');
  const [adminTab, setAdminTab] = useState<'RELATORIOS' | 'GESTAO_QUADRAS' | 'USUARIOS' | 'AUDITORIA'>('RELATORIOS');

  if (loadingAuth) {
    return <FallbackSpinner />;
  }

  if (!user) {
    return (
      <Suspense fallback={<FallbackSpinner />}>
        <AuthPage />
      </Suspense>
    );
  }

  // Mapeamento de abas para o AdminDashboard interno
  const adminTabMapping: Record<string, 'dashboard' | 'quadras' | 'usuarios' | 'auditoria'> = {
    RELATORIOS: 'dashboard',
    GESTAO_QUADRAS: 'quadras',
    USUARIOS: 'usuarios',
    AUDITORIA: 'auditoria',
  };

  const currentTab = isAdmin ? adminTab : clientTab;
  const handleSelectTab = (tab: NavigationTab) => {
    if (isAdmin) {
      setAdminTab(tab as AdminTab);
    } else {
      setClientTab(tab as ClientTab);
    }
  };

  return (
    <DashboardLayout currentTab={currentTab} onSelectTab={handleSelectTab}>
      <Suspense fallback={<FallbackSpinner />}>
        {isAdmin ? (
          <AdminDashboard
            activeTab={adminTabMapping[adminTab] || 'dashboard'}
          />
        ) : (
          <ClientDashboard activeTab={clientTab} />
        )}
      </Suspense>
    </DashboardLayout>
  );
};
