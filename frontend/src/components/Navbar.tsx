import React, { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { LogOut, UserCircle2, Shield, KeyRound, Terminal } from 'lucide-react';
import { Logo, ChangePasswordModal, ApiKeyModal, Badge } from './ui';

export const Navbar: React.FC = () => {
  const { user, logout, isAdmin } = useAuth();
  const [modalSenhaOpen, setModalSenhaOpen] = useState(false);
  const [modalApiKeyOpen, setModalApiKeyOpen] = useState(false);

  return (
    <>
      <header className="border-b border-surface-800/80 bg-surface-950/80 backdrop-blur-xl sticky top-0 z-40 transition-all">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Logo size={36} />
          </div>

          {user && (
            <div className="flex items-center gap-2 sm:gap-3">
              {/* Usuário logado e Badge de Role */}
              <div className="flex items-center gap-2.5 px-3 py-1.5 rounded-xl bg-surface-900/80 border border-surface-800/80">
                <div className="relative flex items-center justify-center">
                  {isAdmin ? (
                    <Shield className="w-4 h-4 text-brand-400" />
                  ) : (
                    <UserCircle2 className="w-4 h-4 text-surface-400" />
                  )}
                  <span className="w-2 h-2 rounded-full bg-emerald-400 absolute -bottom-0.5 -right-0.5 ring-2 ring-surface-950 shadow-[0_0_6px_#34d399]" />
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-xs sm:text-sm font-semibold text-white max-w-[120px] sm:max-w-[180px] truncate">
                    {user.nome_usuario}
                  </span>
                  <Badge variant={isAdmin ? 'success' : 'neutral'} className="hidden sm:inline-flex text-[9px] py-0 px-1.5">
                    {isAdmin ? 'ADMIN' : 'CLIENTE'}
                  </Badge>
                </div>
              </div>

              <div className="h-5 w-[1px] bg-surface-800 hidden sm:block" />

              {/* Botão de API-KEY */}
              <button
                onClick={() => setModalApiKeyOpen(true)}
                className="p-2 sm:px-3 sm:py-2 rounded-xl text-brand-400 hover:text-brand-300 hover:bg-surface-900 border border-brand-500/20 hover:border-brand-500/40 transition-all flex items-center gap-1.5 text-xs font-semibold active:scale-95 shadow-sm cursor-pointer"
                title="Visualizar API-KEY"
                aria-label="Visualizar chave de API"
              >
                <Terminal className="w-3.5 h-3.5" />
                <span className="hidden md:inline">API-KEY</span>
              </button>

              {/* Botão de Alterar Senha */}
              <button
                onClick={() => setModalSenhaOpen(true)}
                className="p-2 sm:px-3 sm:py-2 rounded-xl text-surface-300 hover:text-white hover:bg-surface-900 border border-transparent hover:border-surface-800 transition-all flex items-center gap-1.5 text-xs font-semibold active:scale-95 cursor-pointer"
                title="Alterar Senha"
                aria-label="Alterar Senha"
              >
                <KeyRound className="w-3.5 h-3.5" />
                <span className="hidden md:inline">Senha</span>
              </button>

              {/* Botão Sair */}
              <button
                onClick={logout}
                className="p-2 sm:px-3 sm:py-2 rounded-xl text-surface-400 hover:text-red-400 hover:bg-surface-900 border border-transparent hover:border-surface-800 transition-all flex items-center gap-1.5 text-xs font-semibold active:scale-95 cursor-pointer"
                title="Sair da conta"
                aria-label="Sair da conta"
              >
                <LogOut className="w-3.5 h-3.5" />
                <span className="hidden sm:inline">Sair</span>
              </button>
            </div>
          )}
        </div>
      </header>

      {/* Modal de Alteração de Senha */}
      <ChangePasswordModal
        isOpen={modalSenhaOpen}
        onClose={() => setModalSenhaOpen(false)}
      />

      {/* Modal de API-KEY */}
      <ApiKeyModal
        isOpen={modalApiKeyOpen}
        onClose={() => setModalApiKeyOpen(false)}
      />
    </>
  );
};
