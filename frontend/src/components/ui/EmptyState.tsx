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
      className={`flex flex-col items-center justify-center text-center p-8 sm:p-12 rounded-3xl bg-fg/[0.03] border border-fg/[0.06] border-dashed space-y-4 ${className}`}
    >
      <div className="w-13 h-13 rounded-2xl bg-fg/[0.03] border border-fg/[0.1] flex items-center justify-center text-fg/60 shadow-sm">
        <Icon className="w-6 h-6 text-fg/80" />
      </div>

      <div className="space-y-1.5 max-w-sm">
        <h4 className="text-sm sm:text-base font-semibold text-fg tracking-tight">
          {title}
        </h4>
        <p className="text-xs sm:text-sm text-fg/50 leading-relaxed tracking-tight">
          {description}
        </p>
      </div>

      {action && <div className="pt-2">{action}</div>}
      {!action && actionLabel && onAction && (
        <div className="pt-2">
          <button
            type="button"
            onClick={onAction}
            className="inline-flex items-center gap-2 bg-fg text-on-accent text-xs font-semibold px-4 py-2 rounded-xl hover:bg-fg/90 transition-all shadow-sm active:scale-95 cursor-pointer tracking-tight"
          >
            {actionLabel}
          </button>
        </div>
      )}
    </div>
  );
};
