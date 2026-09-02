import React from 'react';
import { CheckCircle, AlertCircle } from 'lucide-react';

export interface FeedbackData {
  type: 'success' | 'error';
  message: string;
}

interface FeedbackBannerProps {
  feedback: FeedbackData | null;
  onClose: () => void;
}

export const FeedbackBanner: React.FC<FeedbackBannerProps> = ({ feedback, onClose }) => {
  if (!feedback) return null;

  const isSuccess = feedback.type === 'success';

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-150"
    >
      <div className="bg-zinc-950 border border-zinc-800 rounded-3xl w-full max-w-md p-7 shadow-2xl space-y-6 text-center flex flex-col items-center">
        <div
          className={`p-4 rounded-2xl border ${
            isSuccess
              ? 'bg-emerald-950/30 border-emerald-800/50 text-emerald-400'
              : 'bg-red-950/40 border-red-900/60 text-red-400'
          }`}
        >
          {isSuccess ? (
            <CheckCircle className="w-9 h-9 text-emerald-400" />
          ) : (
            <AlertCircle className="w-9 h-9 text-red-400" />
          )}
        </div>

        <div className="space-y-2.5 w-full">
          <h3 className="text-lg font-bold text-white tracking-tight">
            {isSuccess ? 'Ação Concluída' : 'Atenção / Erro'}
          </h3>
          <p className="text-sm text-zinc-300 leading-relaxed whitespace-pre-line break-words max-w-sm mx-auto">
            {feedback.message}
          </p>
        </div>

        <div className="w-full pt-1">
          <button
            type="button"
            onClick={onClose}
            className={`w-full font-bold py-3 rounded-xl text-xs transition shadow-lg active:scale-[0.98] ${
              isSuccess
                ? 'bg-white hover:bg-zinc-200 text-zinc-950'
                : 'bg-red-600 hover:bg-red-500 text-white'
            }`}
          >
            Entendido
          </button>
        </div>
      </div>
    </div>
  );
};
