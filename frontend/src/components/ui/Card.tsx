import React from 'react';

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'subtle' | 'elevated' | 'glass';
  interactive?: boolean;
}

export const Card = React.forwardRef<HTMLDivElement, CardProps>(
  ({ children, className = '', variant = 'default', interactive = false, ...props }, ref) => {
    const variantStyles = {
      default: 'bg-fg/[0.03] border border-fg/[0.06]',
      subtle: 'bg-transparent border border-fg/[0.06]',
      elevated: 'bg-fg/[0.06] border border-fg/[0.1]',
      glass: 'bg-fg/[0.03] backdrop-blur-xl border border-fg/[0.06]',
    };

    const interactiveStyles = interactive
      ? 'transition-all duration-200 hover:border-fg/[0.18] hover:bg-fg/[0.06] cursor-pointer active:scale-[0.99]'
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
