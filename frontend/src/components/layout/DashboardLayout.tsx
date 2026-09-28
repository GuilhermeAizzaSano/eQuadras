import React, { useState } from 'react';
import { Sidebar, NavigationTab } from './Sidebar';
import {
  ChangePasswordModal,
  ApiKeyModal,
  NavBar,
  NavItem,
  Avatar,
  AvatarFallback,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
  Logo,
} from '../ui';
import { useAuth } from '../../contexts/AuthContext';
import { useTema } from '../../shared/theme/useTema';
import {
  MapPin,
  Clock,
  LayoutDashboard,
  Settings2,
  Users,
  ShieldAlert,
  KeyRound,
  Terminal,
  LogOut,
  Sun,
  Moon,
} from 'lucide-react';

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
  const { user, isAdmin, isMasterAdmin, logout } = useAuth();
  const { tema, alternar: alternarTema } = useTema();
  const [modalSenhaOpen, setModalSenhaOpen] = useState(false);
  const [modalApiKeyOpen, setModalApiKeyOpen] = useState(false);

  const getInitials = (name?: string) => {
    if (!name) return 'EQ';
    const parts = name.trim().split(' ');
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  };

  // Montagem dos itens de navegação da Tubelight Navbar para Mobile
  const navItems: NavItem[] = !isAdmin
    ? [
        {
          name: 'Quadras',
          id: 'QUADRAS',
          icon: MapPin,
          onClick: () => onSelectTab('QUADRAS'),
        },
        {
          name: 'Minhas Agendas',
          id: 'AGENDAS',
          icon: Clock,
          onClick: () => onSelectTab('AGENDAS'),
        },
      ]
    : [
        {
          name: 'Relatórios',
          id: 'RELATORIOS',
          icon: LayoutDashboard,
          onClick: () => onSelectTab('RELATORIOS'),
        },
        {
          name: 'Quadras',
          id: 'GESTAO_QUADRAS',
          icon: Settings2,
          onClick: () => onSelectTab('GESTAO_QUADRAS'),
        },
        ...(isMasterAdmin
          ? [
              {
                name: 'Usuários',
                id: 'USUARIOS',
                icon: Users,
                onClick: () => onSelectTab('USUARIOS'),
              },
              {
                name: 'Auditoria',
                id: 'AUDITORIA',
                icon: ShieldAlert,
                onClick: () => onSelectTab('AUDITORIA'),
              },
            ]
          : []),
      ];

  return (
    <div className="flex h-screen w-screen overflow-hidden bg-bg text-fg selection:bg-fg/20 selection:text-fg">
      {/* Barra de Topo exclusiva para Mobile */}
      <header className="flex md:hidden fixed top-0 left-0 right-0 z-40 h-14 items-center justify-between px-4 bg-bg/85 backdrop-blur-md border-b border-fg/10 select-none">
        <div className="flex items-center gap-2.5">
          <Logo size={24} showText={false} />
          <span className="text-sm font-semibold tracking-tight text-fg">eQuadras</span>
        </div>

        <DropdownMenu modal={false}>
          <DropdownMenuTrigger asChild>
            <button
              type="button"
              className="flex items-center justify-center p-1 rounded-full outline-none hover:bg-fg/[0.06] transition"
              title={user?.nome_usuario || 'Conta'}
            >
              <Avatar className="size-8 bg-fg/[0.1] border border-fg/[0.15] text-fg">
                <AvatarFallback>{getInitials(user?.nome_usuario)}</AvatarFallback>
              </Avatar>
            </button>
          </DropdownMenuTrigger>

          <DropdownMenuContent side="bottom" align="end" sideOffset={8} className="w-56 bg-surface-2 border-fg/10 text-fg shadow-apple-elevated">
            <div className="flex items-center gap-2.5 p-2">
              <Avatar className="size-8 bg-fg/[0.1] border border-fg/[0.15] text-fg">
                <AvatarFallback>{getInitials(user?.nome_usuario)}</AvatarFallback>
              </Avatar>
              <div className="flex flex-col min-w-0">
                <span className="text-xs font-semibold text-fg truncate">
                  {user?.nome_usuario}
                </span>
                <span className="text-xs text-fg/60 truncate">
                  {user?.email_usuario}
                </span>
              </div>
            </div>
            <DropdownMenuSeparator className="bg-fg/10" />
            <DropdownMenuItem
              onClick={() => setModalSenhaOpen(true)}
              className="text-xs flex items-center gap-2 cursor-pointer focus:bg-fg/10 focus:text-fg"
            >
              <KeyRound className="size-3.5 text-fg/60" />
              <span>Trocar Senha</span>
            </DropdownMenuItem>
            <DropdownMenuItem
              onClick={() => setModalApiKeyOpen(true)}
              className="text-xs flex items-center gap-2 cursor-pointer focus:bg-fg/10 focus:text-fg"
            >
              <Terminal className="size-3.5 text-fg/60" />
              <span>Gerenciar API-Key</span>
            </DropdownMenuItem>
            <DropdownMenuItem
              onClick={alternarTema}
              className="text-xs flex items-center gap-2 cursor-pointer focus:bg-fg/10 focus:text-fg"
            >
              {tema === 'claro' ? (
                <Moon className="size-3.5 text-fg/60" />
              ) : (
                <Sun className="size-3.5 text-fg/60" />
              )}
              <span>{tema === 'claro' ? 'Tema escuro' : 'Tema claro'}</span>
            </DropdownMenuItem>
            <DropdownMenuSeparator className="bg-fg/10" />
            <DropdownMenuItem
              onClick={logout}
              className="text-xs flex items-center gap-2 cursor-pointer text-danger focus:bg-danger/10 focus:text-danger"
            >
              <LogOut className="size-3.5" />
              <span>Sair da conta</span>
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </header>

      {/* Fixed Left Navigation (Desktop apenas) */}
      <Sidebar
        currentTab={currentTab}
        onSelectTab={onSelectTab}
        onOpenApiKey={() => setModalApiKeyOpen(true)}
        onOpenTrocarSenha={() => setModalSenhaOpen(true)}
      />

      {/* Main Content Area com offset lateral no Desktop e vertical no Mobile */}
      <main className="flex-1 h-screen overflow-y-auto bg-bg pt-14 pb-20 md:pt-0 md:pb-0 md:pl-[3.25rem]">
        {children}
      </main>

      {/* Tubelight Navbar Mobile flutuante na parte inferior */}
      <div className="md:hidden">
        <NavBar
          items={navItems}
          activeId={currentTab}
          onSelect={(tab) => onSelectTab(tab as NavigationTab)}
        />
      </div>

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
