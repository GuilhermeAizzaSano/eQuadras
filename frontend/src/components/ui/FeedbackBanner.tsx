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
            ? 'bg-[#121214]/90 border-[#30D158]/30 text-white shadow-black/40'
            : 'bg-[#121214]/90 border-[#FF453A]/30 text-white shadow-black/40'
        }`}
      >
        <div
          className={`p-2 rounded-xl shrink-0 border ${
            isSuccess
              ? 'bg-[#30D158]/10 border-[#30D158]/25 text-[#30D158]'
              : 'bg-[#FF453A]/10 border-[#FF453A]/25 text-[#FF453A]'
          }`}
        >
          {isSuccess ? (
            <CheckCircle2 className="w-5 h-5 text-[#30D158]" />
          ) : (
            <AlertTriangle className="w-5 h-5 text-[#FF453A]" />
          )}
        </div>

        <div className="flex-1 min-w-0 pt-0.5">
          <p className="text-[11px] font-semibold uppercase tracking-wider text-white/40 mb-0.5">
            {isSuccess ? 'Sucesso' : 'Atenção'}
          </p>
          <p className="text-xs sm:text-sm text-white/90 leading-snug whitespace-pre-line break-words tracking-tight">
            {feedback.message}
          </p>
        </div>

        <button
          type="button"
          onClick={onClose}
          aria-label="Fechar notificação"
          className="p-1 rounded-lg text-white/40 hover:text-white hover:bg-white/[0.08] transition shrink-0"
        >
          <X className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};

