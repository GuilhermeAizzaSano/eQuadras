import React from 'react';

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'subtle' | 'elevated' | 'glass';
  interactive?: boolean;
}

export const Card = React.forwardRef<HTMLDivElement, CardProps>(
  ({ children, className = '', variant = 'default', interactive = false, ...props }, ref) => {
    const variantStyles = {
      default: 'bg-white/[0.03] border border-white/[0.06]',
      subtle: 'bg-transparent border border-white/[0.05]',
      elevated: 'bg-white/[0.05] border border-white/[0.08]',
      glass: 'bg-white/[0.03] backdrop-blur-xl border border-white/[0.07]',
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
