import React, { useState } from 'react';
import { Sidebar, NavigationTab } from './Sidebar';
import { ChangePasswordModal, ApiKeyModal } from '../ui';

interface DashboardLayoutProps {
  children: React.ReactNode;
  currentTab: NavigationTab;
  onSelectTab: (tab: NavigationTab) => void;
}

export const DashboardLayout: React.FC<DashboardLayoutProps> = ({
  children,
  currentTab,
  onSelectTab,
}) => {
  const [modalSenhaOpen, setModalSenhaOpen] = useState(false);
  const [modalApiKeyOpen, setModalApiKeyOpen] = useState(false);

  return (
    <div className="flex h-screen w-screen overflow-hidden bg-black text-white selection:bg-white/20 selection:text-white">
      {/* Fixed Left Navigation */}
      <Sidebar
        currentTab={currentTab}
        onSelectTab={onSelectTab}
        onOpenApiKey={() => setModalApiKeyOpen(true)}
        onOpenTrocarSenha={() => setModalSenhaOpen(true)}
      />

      {/* Main Content Area com offset para a barra fixa recolhida */}
      <main className="flex-1 h-screen overflow-y-auto bg-black pl-[3.25rem]">
        {children}
      </main>

      {/* Modais Globais de Segurança */}
      <ChangePasswordModal
        isOpen={modalSenhaOpen}
        onClose={() => setModalSenhaOpen(false)}
      />

      <ApiKeyModal
        isOpen={modalApiKeyOpen}
        onClose={() => setModalApiKeyOpen(false)}
      />
    </div>
  );
};
