import React from 'react';

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'subtle' | 'elevated' | 'glass';
  interactive?: boolean;
}

export const Card = React.forwardRef<HTMLDivElement, CardProps>(
  ({ children, className = '', variant = 'default', interactive = false, ...props }, ref) => {
    const variantStyles = {
      default: 'bg-[#121214] border border-white/[0.08] shadow-apple-card shadow-[inset_0_1px_0_0_rgba(255,255,255,0.05)]',
      subtle: 'bg-white/[0.03] border border-white/[0.06] shadow-sm',
      elevated: 'bg-[#1c1c1e] border border-white/[0.10] shadow-apple-elevated shadow-[inset_0_1px_0_0_rgba(255,255,255,0.08)]',
      glass: 'bg-white/[0.04] backdrop-blur-2xl border border-white/[0.10] shadow-apple-card shadow-[inset_0_1px_0_0_rgba(255,255,255,0.08)]',
    };

    const interactiveStyles = interactive
      ? 'transition-all duration-200 hover:border-white/[0.18] hover:bg-white/[0.06] cursor-pointer active:scale-[0.99]'
      : '';

    return (
      <div
        ref={ref}
        className={`rounded-2xl sm:rounded-3xl ${variantStyles[variant]} ${interactiveStyles} ${className}`}
        {...props}
      >
        {children}
      </div>
    );
  }
);

Card.displayName = 'Card';
