import React from 'react';

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'subtle' | 'elevated' | 'glass';
  interactive?: boolean;
}

export const Card = React.forwardRef<HTMLDivElement, CardProps>(
  ({ children, className = '', variant = 'default', interactive = false, ...props }, ref) => {
    const variantStyles = {
      default: 'bg-surface-900/80 border border-surface-800/90 shadow-xl',
      subtle: 'bg-surface-950/60 border border-surface-850/80 shadow-md',
      elevated: 'bg-surface-850/90 border border-surface-750/90 shadow-2xl shadow-black/40',
      glass: 'bg-surface-900/60 backdrop-blur-xl border border-white/10 shadow-2xl',
    };

    const interactiveStyles = interactive
      ? 'transition-all duration-300 hover:border-surface-700 hover:bg-surface-900 cursor-pointer active:scale-[0.99]'
      : '';

    return (
      <div
        ref={ref}
        className={`rounded-2xl ${variantStyles[variant]} ${interactiveStyles} ${className}`}
        {...props}
      >
        {children}
      </div>
    );
  }
);

Card.displayName = 'Card';
