import React from 'react';

type BadgeVariant = 'neutral' | 'active' | 'success' | 'danger' | 'warning' | 'outline';

interface BadgeProps {
  children: React.ReactNode;
  variant?: BadgeVariant;
  className?: string;
}

export const Badge: React.FC<BadgeProps> = ({
  children,
  variant = 'neutral',
  className = '',
}) => {
  const baseClasses =
    'inline-flex items-center text-[10px] font-mono uppercase tracking-wider px-2.5 py-0.5 rounded-md border transition-all select-none';

  const variantClasses: Record<BadgeVariant, string> = {
    neutral: 'bg-zinc-900/80 border-zinc-800 text-zinc-300',
    outline: 'bg-transparent border-zinc-800 text-zinc-400',
    active: 'bg-white text-zinc-950 font-bold border-white shadow-sm',
    success: 'bg-emerald-950/40 border-emerald-500/40 text-emerald-400 font-semibold',
    warning: 'bg-amber-950/40 border-amber-500/40 text-amber-400 font-semibold',
    danger: 'bg-red-950/40 border-red-500/40 text-red-400 font-semibold',
  };

  return (
    <span className={`${baseClasses} ${variantClasses[variant]} ${className}`}>
      {children}
    </span>
  );
};
