import React from 'react';
import { LucideIcon } from 'lucide-react';

export interface EmptyStateProps {
  icon: LucideIcon;
  title: string;
  description?: string;
  action?: React.ReactNode;
  actionLabel?: string;
  onAction?: () => void;
  className?: string;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon: Icon,
  title,
  description,
  action,
  actionLabel,
  onAction,
  className = '',
}) => {

  return (
    <div
      className={`flex flex-col items-center justify-center text-center p-8 sm:p-12 rounded-2xl bg-surface-900/60 border border-surface-800/80 border-dashed space-y-4 ${className}`}
    >
      <div className="w-14 h-14 rounded-2xl bg-surface-850/90 border border-surface-750 flex items-center justify-center text-surface-400 shadow-lg shadow-black/40">
        <Icon className="w-7 h-7 text-brand-400" />
      </div>

      <div className="space-y-1.5 max-w-sm">
        <h4 className="text-sm sm:text-base font-bold text-white tracking-tight">
          {title}
        </h4>
        <p className="text-xs sm:text-sm text-surface-400 leading-relaxed">
          {description}
        </p>
      </div>

      {action && <div className="pt-2">{action}</div>}
      {!action && actionLabel && onAction && (
        <div className="pt-2">
          <button
            type="button"
            onClick={onAction}
            className="inline-flex items-center gap-2 bg-white text-surface-950 text-xs font-bold px-4 py-2.5 rounded-xl hover:bg-zinc-200 transition-all shadow-md active:scale-95 cursor-pointer"
          >
            {actionLabel}
          </button>
        </div>
      )}
    </div>
  );
};
