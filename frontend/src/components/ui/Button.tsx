import React from 'react';
import { Loader2 } from 'lucide-react';

export type ButtonVariant = 'primary' | 'secondary' | 'destructive' | 'outline' | 'ghost' | 'subtle';
export type ButtonSize = 'sm' | 'md' | 'lg' | 'icon';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  isLoading?: boolean;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      children,
      className = '',
      variant = 'primary',
      size = 'md',
      isLoading = false,
      leftIcon,
      rightIcon,
      disabled,
      type = 'button',
      ...props
    },
    ref
  ) => {
    const baseStyles =
      'inline-flex items-center justify-center font-medium rounded-xl transition-all duration-150 select-none focus:outline-none focus-visible:ring-2 focus-visible:ring-fg/30 focus-visible:ring-offset-2 focus-visible:ring-offset-bg active:scale-[0.98] disabled:opacity-40 disabled:pointer-events-none disabled:active:scale-100 cursor-pointer tracking-tight';

    const variantStyles: Record<ButtonVariant, string> = {
      primary:
        'bg-fg text-on-accent hover:bg-fg/90 font-semibold shadow-sm active:bg-fg/80',
      secondary:
        'bg-fg/[0.1] hover:bg-fg/[0.15] active:bg-fg/[0.18] text-fg border border-fg/[0.15] shadow-sm backdrop-blur-md',
      destructive:
        'bg-danger hover:bg-danger/90 active:bg-danger/80 text-white font-semibold shadow-sm',
      outline:
        'bg-transparent border border-fg/[0.15] hover:border-fg/[0.2] hover:bg-fg/[0.06] text-fg/90 hover:text-fg',
      ghost:
        'bg-transparent text-fg/70 hover:text-fg hover:bg-fg/[0.06]',
      subtle:
        'bg-fg/[0.03] hover:bg-fg/[0.1] text-fg/80 hover:text-fg border border-fg/[0.1]',
    };

    const sizeStyles: Record<ButtonSize, string> = {
      sm: 'text-xs px-3 py-1.5 gap-1.5 rounded-lg',
      md: 'text-xs sm:text-sm px-4 py-2 gap-2 rounded-xl',
      lg: 'text-sm sm:text-base px-5 py-2.5 gap-2.5 font-semibold rounded-xl',
      icon: 'p-2 rounded-xl',
    };

    return (
      <button
        ref={ref}
        type={type}
        disabled={disabled || isLoading}
        className={`${baseStyles} ${variantStyles[variant]} ${sizeStyles[size]} ${className}`}
        {...props}
      >
        {isLoading ? (
          <Loader2 className="w-4 h-4 animate-spin text-current" />
        ) : (
          leftIcon && <span className="shrink-0">{leftIcon}</span>
        )}
        {children}
        {!isLoading && rightIcon && <span className="shrink-0">{rightIcon}</span>}
      </button>
    );
  }
);

Button.displayName = 'Button';
