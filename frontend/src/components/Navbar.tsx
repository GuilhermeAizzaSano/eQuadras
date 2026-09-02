import React from 'react';
import { useAuth } from '../contexts/AuthContext';
import { LogOut, UserCircle2, Shield } from 'lucide-react';
import { Logo } from './ui';

export const Navbar: React.FC = () => {
  const { user, logout, isAdmin } = useAuth();

  return (
    <header className="border-b border-zinc-850 bg-zinc-950/80 backdrop-blur-xl sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Logo size={34} />
        </div>

        {user && (
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2.5 text-sm text-zinc-300">
              {isAdmin ? (
                <Shield className="w-4 h-4 text-emerald-400" />
              ) : (
                <UserCircle2 className="w-4 h-4 text-zinc-400" />
              )}
              <span className="font-medium text-white">{user.nome_usuario}</span>
            </div>

            <button
              onClick={logout}
              className="p-2 rounded-lg text-zinc-400 hover:text-white hover:bg-zinc-900 border border-transparent hover:border-zinc-800 transition-all flex items-center gap-2 text-xs"
              title="Sair da conta"
            >
              <LogOut className="w-4 h-4" />
              <span className="hidden sm:inline">Sair</span>
            </button>
          </div>
        )}
      </div>
    </header>
  );
};
