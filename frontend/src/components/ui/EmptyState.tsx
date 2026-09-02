import React from 'react';
import { LucideIcon } from 'lucide-react';

interface EmptyStateProps {
  icon: LucideIcon;
  title: string;
  description?: string;
  actionLabel?: string;
  onAction?: () => void;
  className?: string;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon: Icon,
  title,
  description,
  actionLabel,
  onAction,
  className = 'py-12 px-6',
}) => {
  return (
    <div
      className={`text-center border border-dashed border-zinc-850 bg-zinc-950/40 rounded-2xl flex flex-col items-center justify-center space-y-2.5 ${className}`}
    >
      <div className="p-3 rounded-full bg-zinc-900 border border-zinc-800 text-zinc-500 mb-1">
        <Icon className="w-6 h-6" />
      </div>
      <h4 className="text-sm font-semibold text-zinc-200">{title}</h4>
      {description && <p className="text-xs text-zinc-500 max-w-sm">{description}</p>}
      {actionLabel && onAction && (
        <button
          type="button"
          onClick={onAction}
          className="mt-3 inline-flex items-center gap-2 bg-white text-black text-xs font-semibold px-4 py-2 rounded-xl hover:bg-zinc-200 transition shadow-md"
        >
          {actionLabel}
        </button>
      )}
    </div>
  );
};
