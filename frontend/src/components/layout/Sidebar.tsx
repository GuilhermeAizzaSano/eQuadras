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
  UserCircle2,
  Shield
} from 'lucide-react';

export type ClientTab = 'QUADRAS' | 'AGENDAS';
export type AdminTab = 'RELATORIOS' | 'GESTAO_QUADRAS' | 'USUARIOS' | 'AUDITORIA';
export type NavigationTab = ClientTab | AdminTab;

interface SidebarProps {
  currentTab: NavigationTab;
  onSelectTab: (tab: NavigationTab) => void;
  onOpenApiKey: () => void;
  onOpenTrocarSenha: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  currentTab,
  onSelectTab,
  onOpenApiKey,
  onOpenTrocarSenha,
}) => {
  const { user, isAdmin, isMasterAdmin, logout } = useAuth();

  return (
    <aside className="w-64 h-screen bg-black border-r border-white/[0.08] flex flex-col justify-between select-none shrink-0">
      {/* Top Section: Brand & Nav Links */}
      <div className="flex flex-col">
        {/* Brand Header */}
        <div className="h-16 px-6 flex items-center border-b border-white/[0.08]">
          <Logo size={28} />
        </div>

        {/* Navigation Sections */}
        <div className="p-4 space-y-6">
          {/* Main Links */}
          <div>
            <div className="text-[10px] font-mono tracking-widest text-white/40 uppercase px-3 mb-2 font-medium">
              Menu
            </div>
            <nav className="space-y-1">
              {!isAdmin ? (
                // Cliente Nav Items
                <>
                  <button
                    type="button"
                    onClick={() => onSelectTab('QUADRAS')}
                    className={`w-full flex items-center gap-3 px-3 py-2 rounded-xl text-xs font-medium transition cursor-pointer ${
                      currentTab === 'QUADRAS'
                        ? 'bg-white text-black font-semibold shadow-sm'
                        : 'text-white/60 hover:text-white hover:bg-white/[0.04]'
                    }`}
                  >
                    <MapPin className="w-4 h-4 shrink-0" />
                    <span>Quadras</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => onSelectTab('AGENDAS')}
                    className={`w-full flex items-center gap-3 px-3 py-2 rounded-xl text-xs font-medium transition cursor-pointer ${
                      currentTab === 'AGENDAS'
                        ? 'bg-white text-black font-semibold shadow-sm'
                        : 'text-white/60 hover:text-white hover:bg-white/[0.04]'
                    }`}
                  >
                    <Clock className="w-4 h-4 shrink-0" />
                    <span>Minhas Agendas</span>
                  </button>
                </>
              ) : (
                // Admin / Master Nav Items
                <>
                  <button
                    type="button"
                    onClick={() => onSelectTab('RELATORIOS')}
                    className={`w-full flex items-center gap-3 px-3 py-2 rounded-xl text-xs font-medium transition cursor-pointer ${
                      currentTab === 'RELATORIOS'
                        ? 'bg-white text-black font-semibold shadow-sm'
                        : 'text-white/60 hover:text-white hover:bg-white/[0.04]'
                    }`}
                  >
                    <LayoutDashboard className="w-4 h-4 shrink-0" />
                    <span>Relatórios & Agenda</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => onSelectTab('GESTAO_QUADRAS')}
                    className={`w-full flex items-center gap-3 px-3 py-2 rounded-xl text-xs font-medium transition cursor-pointer ${
                      currentTab === 'GESTAO_QUADRAS'
                        ? 'bg-white text-black font-semibold shadow-sm'
                        : 'text-white/60 hover:text-white hover:bg-white/[0.04]'
                    }`}
                  >
                    <Settings2 className="w-4 h-4 shrink-0" />
                    <span>Gestão de Quadras</span>
                  </button>

                  {isMasterAdmin && (
                    <>
                      <div className="pt-3 pb-1">
                        <div className="text-[10px] font-mono tracking-widest text-white/40 uppercase px-3 font-medium">
                          Master
                        </div>
                      </div>
                      <button
                        type="button"
                        onClick={() => onSelectTab('USUARIOS')}
                        className={`w-full flex items-center gap-3 px-3 py-2 rounded-xl text-xs font-medium transition cursor-pointer ${
                          currentTab === 'USUARIOS'
                            ? 'bg-white text-black font-semibold shadow-sm'
                            : 'text-white/60 hover:text-white hover:bg-white/[0.04]'
                        }`}
                      >
                        <Users className="w-4 h-4 shrink-0" />
                        <span>Gestão de Usuários</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => onSelectTab('AUDITORIA')}
                        className={`w-full flex items-center gap-3 px-3 py-2 rounded-xl text-xs font-medium transition cursor-pointer ${
                          currentTab === 'AUDITORIA'
                            ? 'bg-white text-black font-semibold shadow-sm'
                            : 'text-white/60 hover:text-white hover:bg-white/[0.04]'
                        }`}
                      >
                        <ShieldAlert className="w-4 h-4 shrink-0" />
                        <span>Auditoria & Logs</span>
                      </button>
                    </>
                  )}
                </>
              )}
            </nav>
          </div>

          {/* Account / Settings */}
          <div>
            <div className="text-[10px] font-mono tracking-widest text-white/40 uppercase px-3 mb-2 font-medium">
              Conta
            </div>
            <div className="space-y-1">
              <button
                type="button"
                onClick={onOpenApiKey}
                className="w-full flex items-center gap-3 px-3 py-2 rounded-xl text-xs font-medium text-white/60 hover:text-white hover:bg-white/[0.04] transition cursor-pointer"
              >
                <Terminal className="w-4 h-4 shrink-0" />
                <span>API-Key</span>
              </button>
              <button
                type="button"
                onClick={onOpenTrocarSenha}
                className="w-full flex items-center gap-3 px-3 py-2 rounded-xl text-xs font-medium text-white/60 hover:text-white hover:bg-white/[0.04] transition cursor-pointer"
              >
                <KeyRound className="w-4 h-4 shrink-0" />
                <span>Trocar Senha</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Bottom Section: User Profile & Logout */}
      <div className="p-4 border-t border-white/[0.08] space-y-3">
        {user && (
          <div className="flex items-center gap-3 px-2">
            <div className="relative flex items-center justify-center">
              {isAdmin ? (
                <Shield className="w-4 h-4 text-white/70" />
              ) : (
                <UserCircle2 className="w-4 h-4 text-white/60" />
              )}
              <span className="w-1.5 h-1.5 rounded-full bg-[#30D158] absolute -bottom-0.5 -right-0.5" />
            </div>
            <div className="flex-1 min-w-0">
              <div className="font-medium text-xs text-white truncate">
                {user.nome_usuario}
              </div>
              <div className="text-[10px] font-mono uppercase tracking-wider text-white/40">
                {isMasterAdmin ? 'Master Admin' : isAdmin ? 'Admin' : 'Cliente'}
              </div>
            </div>
          </div>
        )}

        <button
          type="button"
          onClick={logout}
          className="w-full flex items-center gap-3 px-3 py-2 rounded-xl text-xs font-medium text-white/40 hover:text-[#FF453A] hover:bg-[#FF453A]/10 transition cursor-pointer active:scale-98"
        >
          <LogOut className="w-4 h-4 shrink-0" />
          <span>Sair</span>
        </button>
      </div>
    </aside>
  );
};
