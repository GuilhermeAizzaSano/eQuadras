import React from 'react';
import { Loader2 } from 'lucide-react';

interface LoadingOverlayProps {
  isLoading: boolean;
  message?: string;
}

export const LoadingOverlay: React.FC<LoadingOverlayProps> = ({
  isLoading,
  message = 'Processando...',
}) => {
  if (!isLoading) return null;

  return (
    <div
      role="alert"
      aria-busy="true"
      className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-in fade-in duration-200"
    >
      <div className="bg-surface-900/95 border border-surface-800 rounded-3xl p-6 sm:p-8 flex flex-col items-center gap-4 max-w-sm w-full shadow-2xl shadow-black/80 text-center animate-in zoom-in-95 duration-200">
        <div className="relative flex items-center justify-center">
          <div className="w-12 h-12 rounded-full border-2 border-brand-500/20 animate-ping absolute inset-0" />
          <div className="w-12 h-12 rounded-2xl bg-surface-850 border border-surface-750 flex items-center justify-center shadow-lg shadow-brand-500/10">
            <Loader2 className="w-6 h-6 text-brand-400 animate-spin" />
          </div>
        </div>
        <div className="space-y-1 flex flex-col items-center">
          <p className="text-xs sm:text-sm font-semibold text-white leading-snug">
            {message}
          </p>
          <span className="text-[11px] text-surface-400">Aguarde um instante</span>
        </div>
      </div>
    </div>
  );
};

