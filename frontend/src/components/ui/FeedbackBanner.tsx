import React, { useEffect } from 'react';
import { CheckCircle2, AlertTriangle, X } from 'lucide-react';

export interface FeedbackData {
  type: 'success' | 'error';
  message: string;
}

interface FeedbackBannerProps {
  feedback: FeedbackData | null;
  onClose: () => void;
}

export const FeedbackBanner: React.FC<FeedbackBannerProps> = ({ feedback, onClose }) => {
  useEffect(() => {
    if (!feedback) return;
    // Auto-dismiss após 5 segundos
    const timer = setTimeout(() => {
      onClose();
    }, 5000);
    return () => clearTimeout(timer);
  }, [feedback, onClose]);

  if (!feedback) return null;

  const isSuccess = feedback.type === 'success';

  return (
    <div className="fixed top-5 right-5 z-[9999] max-w-md w-[calc(100vw-2.5rem)] animate-in slide-in-from-top-4 fade-in duration-300 pointer-events-auto">
      <div
        className={`p-4 rounded-2xl border shadow-2xl backdrop-blur-xl flex items-start gap-3.5 transition-all ${
          isSuccess
            ? 'bg-surface-900/95 border-emerald-500/40 text-surface-100 shadow-emerald-950/40'
            : 'bg-surface-900/95 border-red-500/40 text-surface-100 shadow-red-950/40'
        }`}
      >
        <div
          className={`p-2 rounded-xl shrink-0 border ${
            isSuccess
              ? 'bg-emerald-950/60 border-emerald-500/30 text-emerald-400'
              : 'bg-red-950/60 border-red-500/30 text-red-400'
          }`}
        >
          {isSuccess ? (
            <CheckCircle2 className="w-5 h-5 text-emerald-400" />
          ) : (
            <AlertTriangle className="w-5 h-5 text-red-400" />
          )}
        </div>

        <div className="flex-1 min-w-0 pt-0.5">
          <p className="text-xs font-bold uppercase tracking-wider text-surface-400 mb-0.5">
            {isSuccess ? 'Sucesso' : 'Atenção'}
          </p>
          <p className="text-xs sm:text-sm text-surface-200 leading-snug whitespace-pre-line break-words">
            {feedback.message}
          </p>
        </div>

        <button
          type="button"
          onClick={onClose}
          aria-label="Fechar notificação"
          className="p-1 rounded-lg text-surface-400 hover:text-white hover:bg-surface-800 transition shrink-0"
        >
          <X className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};

