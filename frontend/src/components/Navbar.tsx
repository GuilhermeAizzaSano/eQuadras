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
      <header className="border-b border-white/[0.08] bg-black/75 backdrop-blur-2xl sticky top-0 z-40 transition-all">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Logo size={36} />
          </div>

          {user && (
            <div className="flex items-center gap-2 sm:gap-2.5">
              {/* Usuário logado e Badge de Role */}
              <div className="flex items-center gap-2.5 px-3 py-1.5 rounded-full bg-white/[0.05] border border-white/[0.08] backdrop-blur-md">
                <div className="relative flex items-center justify-center">
                  {isAdmin ? (
                    <Shield className="w-4 h-4 text-white/80" />
                  ) : (
                    <UserCircle2 className="w-4 h-4 text-white/60" />
                  )}
                  <span className="w-2 h-2 rounded-full bg-[#30D158] absolute -bottom-0.5 -right-0.5 ring-2 ring-black" />
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-xs sm:text-sm font-medium text-white max-w-[120px] sm:max-w-[180px] truncate tracking-tight">
                    {user.nome_usuario}
                  </span>
                  <Badge variant={isAdmin ? 'success' : 'neutral'} className="hidden sm:inline-flex text-[10px] py-0.5 px-2">
                    {isAdmin ? 'ADMIN' : 'CLIENTE'}
                  </Badge>
                </div>
              </div>

              <div className="h-5 w-[1px] bg-white/[0.08] hidden sm:block mx-1" />

              {/* Botão de API-KEY */}
              <button
                onClick={() => setModalApiKeyOpen(true)}
                className="p-2 sm:px-3 sm:py-1.5 rounded-full text-white/80 hover:text-white bg-white/[0.04] hover:bg-white/[0.08] border border-white/[0.08] hover:border-white/[0.15] transition-all flex items-center gap-1.5 text-xs font-medium active:scale-95 shadow-sm cursor-pointer tracking-tight"
                title="Visualizar API-KEY"
                aria-label="Visualizar chave de API"
              >
                <Terminal className="w-3.5 h-3.5 text-white/70" />
                <span className="hidden md:inline">API-KEY</span>
              </button>

              {/* Botão de Alterar Senha */}
              <button
                onClick={() => setModalSenhaOpen(true)}
                className="p-2 sm:px-3 sm:py-1.5 rounded-full text-white/70 hover:text-white bg-transparent hover:bg-white/[0.06] border border-transparent hover:border-white/[0.08] transition-all flex items-center gap-1.5 text-xs font-medium active:scale-95 cursor-pointer tracking-tight"
                title="Alterar Senha"
                aria-label="Alterar Senha"
              >
                <KeyRound className="w-3.5 h-3.5 text-white/60" />
                <span className="hidden md:inline">Senha</span>
              </button>

              {/* Botão Sair */}
              <button
                onClick={logout}
                className="p-2 sm:px-3 sm:py-1.5 rounded-full text-white/50 hover:text-[#FF453A] bg-transparent hover:bg-[#FF453A]/10 border border-transparent hover:border-[#FF453A]/20 transition-all flex items-center gap-1.5 text-xs font-medium active:scale-95 cursor-pointer tracking-tight"
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
