import React from 'react';
import { AlertTriangle, CalendarCheck, X } from 'lucide-react';
import { Button } from './Button';

export interface ConfirmModalProps {
  isOpen: boolean;
  title: string;
  description: string;
  confirmLabel?: string;
  cancelLabel?: string;
  isDestructive?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

export const ConfirmModal: React.FC<ConfirmModalProps> = ({
  isOpen,
  title,
  description,
  confirmLabel = 'Confirmar',
  cancelLabel = 'Cancelar',
  isDestructive = false,
  onConfirm,
  onCancel,
}) => {
  if (!isOpen) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-in fade-in duration-200"
    >
      <div className="bg-surface-900 border border-surface-800 rounded-3xl w-full max-w-md p-6 sm:p-7 shadow-2xl shadow-black/80 space-y-6 animate-in zoom-in-95 duration-200">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <div
              className={`p-3 rounded-2xl border shrink-0 ${
                isDestructive
                  ? 'bg-red-950/50 border-red-500/30 text-red-400 shadow-md shadow-red-950/40'
                  : 'bg-emerald-950/50 border-emerald-500/30 text-emerald-400 shadow-md shadow-emerald-950/40'
              }`}
            >
              {isDestructive ? (
                <AlertTriangle className="w-5 h-5" />
              ) : (
                <CalendarCheck className="w-5 h-5" />
              )}
            </div>
            <h3 className="text-lg font-bold text-white tracking-tight leading-tight">{title}</h3>
          </div>
          <button
            type="button"
            onClick={onCancel}
            className="p-1.5 rounded-xl text-surface-400 hover:text-white hover:bg-surface-800 transition shrink-0"
            aria-label="Fechar modal"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <p className="text-xs sm:text-sm text-surface-300 leading-relaxed whitespace-pre-line break-words">
          {description}
        </p>

        <div className="flex items-center gap-3 pt-2">
          <Button
            type="button"
            variant="outline"
            onClick={onCancel}
            className="flex-1"
          >
            {cancelLabel}
          </Button>
          <Button
            type="button"
            variant={isDestructive ? 'destructive' : 'primary'}
            onClick={onConfirm}
            className="flex-1"
          >
            {confirmLabel}
          </Button>
        </div>
      </div>
    </div>
  );
};

