import React from 'react';

export type BadgeVariant = 'neutral' | 'active' | 'success' | 'danger' | 'warning' | 'outline' | 'info';

export interface BadgeProps {
  children: React.ReactNode;
  variant?: BadgeVariant;
  className?: string;
  withDot?: boolean;
}

export const Badge: React.FC<BadgeProps> = ({
  children,
  variant = 'neutral',
  className = '',
  withDot = false,
}) => {
  const baseClasses =
    'inline-flex items-center gap-1.5 text-[10px] font-mono uppercase tracking-wider px-2.5 py-0.5 rounded-md border transition-all select-none font-semibold';

  const variantClasses: Record<BadgeVariant, { container: string; dot: string }> = {
    neutral: {
      container: 'bg-surface-900/90 border-surface-800 text-surface-300',
      dot: 'bg-surface-400',
    },
    outline: {
      container: 'bg-transparent border-surface-800 text-surface-400',
      dot: 'bg-surface-500',
    },
    active: {
      container: 'bg-white text-surface-950 font-bold border-white shadow-sm',
      dot: 'bg-surface-950',
    },
    success: {
      container: 'bg-emerald-950/40 border-emerald-500/40 text-emerald-400',
      dot: 'bg-emerald-400 shadow-[0_0_6px_#34d399]',
    },
    warning: {
      container: 'bg-amber-950/40 border-amber-500/40 text-amber-300',
      dot: 'bg-amber-400 shadow-[0_0_6px_#fbbf24]',
    },
    danger: {
      container: 'bg-red-950/40 border-red-500/40 text-red-400',
      dot: 'bg-red-400 shadow-[0_0_6px_#f87171]',
    },
    info: {
      container: 'bg-sky-950/40 border-sky-500/40 text-sky-400',
      dot: 'bg-sky-400 shadow-[0_0_6px_#38bdf8]',
    },
  };

  const selected = variantClasses[variant] || variantClasses.neutral;

  return (
    <span className={`${baseClasses} ${selected.container} ${className}`}>
      {withDot && (
        <span className={`w-1.5 h-1.5 rounded-full shrink-0 ${selected.dot}`} />
      )}
      {children}
    </span>
  );
};

