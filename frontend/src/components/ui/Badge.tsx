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
      container: 'bg-white/[0.06] border-white/[0.08] text-white/70',
      dot: 'bg-white/40',
    },
    outline: {
      container: 'bg-transparent border-white/[0.12] text-white/60',
      dot: 'bg-white/40',
    },
    active: {
      container: 'bg-white text-black font-semibold border-white shadow-sm',
      dot: 'bg-black',
    },
    success: {
      container: 'bg-[#30D158]/10 border-[#30D158]/25 text-[#30D158]',
      dot: 'bg-[#30D158]',
    },
    warning: {
      container: 'bg-[#FF9F0A]/10 border-[#FF9F0A]/25 text-[#FF9F0A]',
      dot: 'bg-[#FF9F0A]',
    },
    danger: {
      container: 'bg-[#FF453A]/10 border-[#FF453A]/25 text-[#FF453A]',
      dot: 'bg-[#FF453A]',
    },
    info: {
      container: 'bg-[#0A84FF]/10 border-[#0A84FF]/25 text-[#0A84FF]',
      dot: 'bg-[#0A84FF]',
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

