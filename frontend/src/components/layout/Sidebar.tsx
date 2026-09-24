import React from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { Logo } from '../ui/Logo';
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
  ChevronsUpDown,
  Sun,
  Moon,
} from 'lucide-react';
import { useTema } from '../../shared/theme/useTema';
import {
  Sidebar as SidebarPrimitive,
  SidebarItemText,
  useSidebar,
} from '../ui/SidebarPrimitives';
import {
  Avatar,
  AvatarFallback,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
  Separator,
} from '../ui';
import { cn } from '../../lib/utils';

export type ClientTab = 'QUADRAS' | 'AGENDAS';
export type AdminTab = 'RELATORIOS' | 'GESTAO_QUADRAS' | 'USUARIOS' | 'AUDITORIA';
export type NavigationTab = ClientTab | AdminTab;

export interface SidebarProps {
  currentTab: NavigationTab;
  onSelectTab: (tab: NavigationTab) => void;
  onOpenApiKey: () => void;
  onOpenTrocarSenha: () => void;
}

const SidebarHeader: React.FC<{ onSelectHome: () => void }> = ({
  onSelectHome,
}) => {
  const { isCollapsed } = useSidebar();

  return (
    <div className="flex h-14 w-full items-center px-2">
      <DropdownMenu modal={false}>
        <DropdownMenuTrigger asChild className="w-full">
          <button
            type="button"
            className="flex w-full items-center gap-2.5 rounded-lg p-1.5 transition hover:bg-fg/[0.06] text-left outline-none cursor-pointer"
          >
            <div className="flex size-7 shrink-0 items-center justify-center rounded-lg bg-fg/[0.1] border border-fg/[0.15]">
              <Logo size={20} showText={false} />
            </div>
            {!isCollapsed && (
              <div className="flex min-w-0 flex-1 items-center justify-between">
                <div className="flex flex-col truncate">
                  <span className="text-xs font-semibold text-fg tracking-tight truncate">
                    eQuadras
                  </span>
                  <span className="text-xs text-fg/60 truncate">
                    Sistema de Gestão
                  </span>
                </div>
                <ChevronsUpDown className="size-3.5 shrink-0 text-fg/60 ml-1" />
              </div>
            )}
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="start" className="w-48 bg-surface-2 border-fg/10 text-fg">
          <div className="px-2 py-1.5 text-xs font-medium text-fg/60">
            Ambiente Ativo
          </div>
          <DropdownMenuItem
            className="text-xs focus:bg-fg/10 focus:text-fg cursor-pointer"
            onClick={onSelectHome}
          >
            <div className="flex items-center gap-2">
              <span className="size-2 rounded-full bg-success" />
              <span>eQuadras Principal</span>
            </div>
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
};

const SidebarFooter: React.FC<{
  onOpenApiKey: () => void;
  onOpenTrocarSenha: () => void;
}> = ({ onOpenApiKey, onOpenTrocarSenha }) => {
  const { user, isAdmin, isMasterAdmin, logout } = useAuth();
  const { tema, alternar: alternarTema } = useTema();
  const { isCollapsed } = useSidebar();

  const getInitials = (name?: string) => {
    if (!name) return 'EQ';
    const parts = name.trim().split(' ');
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  };

  return (
    <DropdownMenu modal={false}>
      <DropdownMenuTrigger asChild className="w-full">
        <button
          type="button"
          className="flex h-10 w-full items-center gap-2.5 rounded-xl px-1.5 py-1 transition hover:bg-fg/[0.06] text-left outline-none cursor-pointer"
          title={user?.nome_usuario || 'Conta'}
        >
          <div className="relative flex size-7 shrink-0 items-center justify-center">
            <Avatar className="size-7 bg-fg/[0.1] border border-fg/[0.15] text-fg">
              <AvatarFallback>{getInitials(user?.nome_usuario)}</AvatarFallback>
            </Avatar>
            <span className="absolute -bottom-0.5 -right-0.5 size-2 rounded-full bg-success ring-2 ring-bg" />
          </div>

          {!isCollapsed && (
            <div className="flex min-w-0 flex-1 items-center justify-between">
              <div className="flex flex-col truncate">
                <span className="text-xs font-medium text-fg truncate">
                  {user?.nome_usuario || 'Usuário'}
                </span>
                <span className="text-xs font-mono uppercase tracking-wider text-fg/60 truncate">
                  {isMasterAdmin ? 'Master Admin' : isAdmin ? 'Admin' : 'Cliente'}
                </span>
              </div>
              <ChevronsUpDown className="size-3.5 shrink-0 text-fg/60 ml-1" />
            </div>
          )}
        </button>
      </DropdownMenuTrigger>

      <DropdownMenuContent side="right" align="end" sideOffset={10} className="w-56 bg-surface-2 border-fg/10 text-fg shadow-apple-elevated">
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
          onClick={onOpenTrocarSenha}
          className="text-xs flex items-center gap-2 cursor-pointer focus:bg-fg/10 focus:text-fg"
        >
          <KeyRound className="size-3.5 text-fg/60" />
          <span>Trocar Senha</span>
        </DropdownMenuItem>
        <DropdownMenuItem
          onClick={onOpenApiKey}
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
  );
};

export const Sidebar: React.FC<SidebarProps> = ({
  currentTab,
  onSelectTab,
  onOpenApiKey,
  onOpenTrocarSenha,
}) => {
  const { isAdmin, isMasterAdmin } = useAuth();

  return (
    <SidebarPrimitive
      header={
        <SidebarHeader
          onSelectHome={() => onSelectTab(isAdmin ? 'RELATORIOS' : 'QUADRAS')}
        />
      }
      footer={
        <SidebarFooter
          onOpenApiKey={onOpenApiKey}
          onOpenTrocarSenha={onOpenTrocarSenha}
        />
      }
    >
      <div className="flex w-full flex-col gap-1">
        {!isAdmin ? (
          <>
            <button
              type="button"
              onClick={() => onSelectTab('QUADRAS')}
              className={cn(
                'flex h-9 w-full items-center rounded-xl px-2.5 py-2 transition text-xs font-medium cursor-pointer',
                currentTab === 'QUADRAS'
                  ? 'bg-fg text-on-accent font-semibold shadow-sm'
                  : 'text-fg/60 hover:text-fg hover:bg-fg/[0.03]'
              )}
              title="Quadras"
            >
              <MapPin className="size-4 shrink-0" />
              <SidebarItemText className="ml-3">Quadras</SidebarItemText>
            </button>

            <button
              type="button"
              onClick={() => onSelectTab('AGENDAS')}
              className={cn(
                'flex h-9 w-full items-center rounded-xl px-2.5 py-2 transition text-xs font-medium cursor-pointer',
                currentTab === 'AGENDAS'
                  ? 'bg-fg text-on-accent font-semibold shadow-sm'
                  : 'text-fg/60 hover:text-fg hover:bg-fg/[0.03]'
              )}
              title="Minhas Agendas"
            >
              <Clock className="size-4 shrink-0" />
              <SidebarItemText className="ml-3">Minhas Agendas</SidebarItemText>
            </button>
          </>
        ) : (
          <>
            <button
              type="button"
              onClick={() => onSelectTab('RELATORIOS')}
              className={cn(
                'flex h-9 w-full items-center rounded-xl px-2.5 py-2 transition text-xs font-medium cursor-pointer',
                currentTab === 'RELATORIOS'
                  ? 'bg-fg text-on-accent font-semibold shadow-sm'
                  : 'text-fg/60 hover:text-fg hover:bg-fg/[0.03]'
              )}
              title="Relatórios & Agenda"
            >
              <LayoutDashboard className="size-4 shrink-0" />
              <SidebarItemText className="ml-3">Relatórios & Agenda</SidebarItemText>
            </button>

            <button
              type="button"
              onClick={() => onSelectTab('GESTAO_QUADRAS')}
              className={cn(
                'flex h-9 w-full items-center rounded-xl px-2.5 py-2 transition text-xs font-medium cursor-pointer',
                currentTab === 'GESTAO_QUADRAS'
                  ? 'bg-fg text-on-accent font-semibold shadow-sm'
                  : 'text-fg/60 hover:text-fg hover:bg-fg/[0.03]'
              )}
              title="Gestão de Quadras"
            >
              <Settings2 className="size-4 shrink-0" />
              <SidebarItemText className="ml-3">Gestão de Quadras</SidebarItemText>
            </button>

            {isMasterAdmin && (
              <>
                <div className="py-1">
                  <Separator className="bg-fg/[0.1]" />
                </div>
                <button
                  type="button"
                  onClick={() => onSelectTab('USUARIOS')}
                  className={cn(
                    'flex h-9 w-full items-center rounded-xl px-2.5 py-2 transition text-xs font-medium cursor-pointer',
                    currentTab === 'USUARIOS'
                      ? 'bg-fg text-on-accent font-semibold shadow-sm'
                      : 'text-fg/60 hover:text-fg hover:bg-fg/[0.03]'
                  )}
                  title="Gestão de Usuários"
                >
                  <Users className="size-4 shrink-0" />
                  <SidebarItemText className="ml-3">Gestão de Usuários</SidebarItemText>
                </button>

                <button
                  type="button"
                  onClick={() => onSelectTab('AUDITORIA')}
                  className={cn(
                    'flex h-9 w-full items-center rounded-xl px-2.5 py-2 transition text-xs font-medium cursor-pointer',
                    currentTab === 'AUDITORIA'
                      ? 'bg-fg text-on-accent font-semibold shadow-sm'
                      : 'text-fg/60 hover:text-fg hover:bg-fg/[0.03]'
                  )}
                  title="Auditoria & Logs"
                >
                  <ShieldAlert className="size-4 shrink-0" />
                  <SidebarItemText className="ml-3">Auditoria & Logs</SidebarItemText>
                </button>
              </>
            )}
          </>
        )}
      </div>
    </SidebarPrimitive>
  );
};
