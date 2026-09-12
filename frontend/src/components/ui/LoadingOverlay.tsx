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
      className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-2xl animate-in fade-in duration-200"
    >
      <div className="bg-[#121214]/90 border border-white/[0.1] rounded-3xl p-6 sm:p-8 flex flex-col items-center gap-4 max-w-xs w-full shadow-apple-elevated text-center animate-in zoom-in-95 duration-200">
        <div className="w-12 h-12 rounded-2xl bg-white/[0.06] border border-white/[0.1] flex items-center justify-center shadow-sm">
          <Loader2 className="w-6 h-6 text-white/80 animate-spin" />
        </div>
        <div className="space-y-1 flex flex-col items-center">
          <p className="text-xs sm:text-sm font-semibold text-white leading-snug tracking-tight">
            {message}
          </p>
          <span className="text-[11px] text-white/40 tracking-tight">Aguarde um instante</span>
        </div>
      </div>
    </div>
  );
};

