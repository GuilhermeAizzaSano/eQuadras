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
    'inline-flex items-center gap-1.5 text-[11px] font-medium tracking-tight px-2.5 py-0.5 rounded-full border transition-all select-none';

  const variantClasses: Record<BadgeVariant, { container: string; dot: string }> = {
    neutral: {
      container: 'bg-fg/[0.06] border-fg/[0.1] text-fg/70',
      dot: 'bg-fg/40',
    },
    outline: {
      container: 'bg-transparent border-fg/[0.15] text-fg/60',
      dot: 'bg-fg/40',
    },
    active: {
      container: 'bg-fg text-on-accent font-semibold border-fg shadow-sm',
      dot: 'bg-bg',
    },
    success: {
      container: 'bg-success/10 border-success/25 text-success',
      dot: 'bg-success',
    },
    warning: {
      container: 'bg-warning/10 border-warning/25 text-warning',
      dot: 'bg-warning',
    },
    danger: {
      container: 'bg-danger/10 border-danger/25 text-danger',
      dot: 'bg-danger',
    },
    info: {
      container: 'bg-info/10 border-info/25 text-info',
      dot: 'bg-info',
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

