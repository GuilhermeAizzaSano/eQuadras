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
        className={`p-4 rounded-2xl border shadow-apple-elevated backdrop-blur-2xl flex items-start gap-3.5 transition-all ${
          isSuccess
            ? 'bg-surface-2/90 border-success/30 text-fg shadow-black/40'
            : 'bg-surface-2/90 border-danger/30 text-fg shadow-black/40'
        }`}
      >
        <div
          className={`p-2 rounded-xl shrink-0 border ${
            isSuccess
              ? 'bg-success/10 border-success/25 text-success'
              : 'bg-danger/10 border-danger/25 text-danger'
          }`}
        >
          {isSuccess ? (
            <CheckCircle2 className="w-5 h-5 text-success" />
          ) : (
            <AlertTriangle className="w-5 h-5 text-danger" />
          )}
        </div>

        <div className="flex-1 min-w-0 pt-0.5">
          <p className="text-[11px] font-semibold uppercase tracking-wider text-fg/50 mb-1 font-mono">
            {isSuccess ? 'Sucesso' : 'Atenção'}
          </p>
          <p className="text-xs sm:text-sm text-fg/95 leading-relaxed whitespace-pre-line break-words font-sans">
            {feedback.message}
          </p>
        </div>

        <button
          type="button"
          onClick={onClose}
          aria-label="Fechar notificação"
          className="p-1 rounded-lg text-fg/40 hover:text-fg hover:bg-fg/[0.1] transition shrink-0"
        >
          <X className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};

