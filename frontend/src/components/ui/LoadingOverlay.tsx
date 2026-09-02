import React from 'react';
import { Loader2 } from 'lucide-react';
import { Logo } from './Logo';

interface LoadingOverlayProps {
  isLoading: boolean;
  message?: string;
}

export const LoadingOverlay: React.FC<LoadingOverlayProps> = ({
  isLoading,
  message = 'Processando requisição...',
}) => {
  if (!isLoading) return null;

  return (
    <div
      role="status"
      aria-live="polite"
      className="fixed inset-0 z-[9999] flex flex-col items-center justify-center bg-black/85 backdrop-blur-md animate-in fade-in duration-200 select-none"
    >
      <div className="relative flex flex-col items-center justify-center p-8 rounded-3xl bg-zinc-950/90 border border-zinc-800 shadow-2xl space-y-4 max-w-xs w-full text-center">
        {/* Glow e Logo pulsante */}
        <div className="relative flex items-center justify-center">
          <div className="absolute w-16 h-16 rounded-full bg-emerald-500/10 blur-xl animate-pulse" />
          <Logo size={44} showText={false} className="animate-pulse" />
        </div>

        {/* Spinner e Texto */}
        <div className="space-y-1.5 flex flex-col items-center">
          <div className="flex items-center gap-2 text-emerald-400 text-xs font-semibold">
            <Loader2 className="w-4 h-4 animate-spin text-emerald-400" />
            <span>Aguarde um instante</span>
          </div>
          <p className="text-xs text-zinc-400 font-medium leading-relaxed max-w-[220px]">
            {message}
          </p>
        </div>
      </div>
    </div>
  );
};
