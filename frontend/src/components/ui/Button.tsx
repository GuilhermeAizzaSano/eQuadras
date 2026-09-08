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
      'inline-flex items-center justify-center font-medium rounded-xl transition-all duration-200 select-none focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-400 focus-visible:ring-offset-2 focus-visible:ring-offset-surface-950 active:scale-[0.98] disabled:opacity-50 disabled:pointer-events-none disabled:active:scale-100 cursor-pointer';

    const variantStyles: Record<ButtonVariant, string> = {
      primary:
        'bg-white text-surface-950 hover:bg-zinc-200 font-bold shadow-md shadow-white/5',
      secondary:
        'bg-brand-500 hover:bg-brand-400 text-surface-950 font-bold shadow-lg shadow-brand-500/20 hover:shadow-brand-500/30',
      destructive:
        'bg-red-600 hover:bg-red-500 text-white font-semibold shadow-md shadow-red-950/40',
      outline:
        'bg-transparent border border-surface-800 hover:border-surface-700 text-surface-200 hover:text-white hover:bg-surface-900/60',
      ghost:
        'bg-transparent text-surface-400 hover:text-white hover:bg-surface-900/80',
      subtle:
        'bg-surface-850/80 hover:bg-surface-800 text-surface-200 hover:text-white border border-surface-800/80',
    };

    const sizeStyles: Record<ButtonSize, string> = {
      sm: 'text-xs px-3 py-1.5 gap-1.5',
      md: 'text-xs sm:text-sm px-4 py-2.5 gap-2',
      lg: 'text-sm sm:text-base px-5 py-3 gap-2.5 font-semibold',
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
