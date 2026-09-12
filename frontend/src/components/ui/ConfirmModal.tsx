import React from 'react';
import { createPortal } from 'react-dom';
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

  return createPortal(
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-2xl overflow-y-auto animate-in fade-in duration-200"
    >
      <div className="bg-[#121214] border border-white/[0.1] rounded-3xl w-full max-w-md p-6 sm:p-7 shadow-apple-elevated shadow-[inset_0_1px_0_0_rgba(255,255,255,0.08)] space-y-6 relative my-auto max-h-[90vh] overflow-y-auto animate-in zoom-in-95 duration-200">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <div
              className={`p-3 rounded-2xl border shrink-0 ${
                isDestructive
                  ? 'bg-[#FF453A]/10 border-[#FF453A]/25 text-[#FF453A]'
                  : 'bg-[#30D158]/10 border-[#30D158]/25 text-[#30D158]'
              }`}
            >
              {isDestructive ? (
                <AlertTriangle className="w-5 h-5" />
              ) : (
                <CalendarCheck className="w-5 h-5" />
              )}
            </div>
            <h3 className="text-lg font-semibold text-white tracking-tight leading-tight">{title}</h3>
          </div>
          <button
            type="button"
            onClick={onCancel}
            className="p-1.5 rounded-xl text-white/40 hover:text-white hover:bg-white/[0.08] transition shrink-0"
            aria-label="Fechar modal"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <p className="text-xs sm:text-sm text-white/60 leading-relaxed whitespace-pre-line break-words tracking-tight">
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
    </div>,
    document.body
  );
};

