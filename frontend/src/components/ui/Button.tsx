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
      'inline-flex items-center justify-center font-medium rounded-xl transition-all duration-150 select-none focus:outline-none focus-visible:ring-2 focus-visible:ring-white/30 focus-visible:ring-offset-2 focus-visible:ring-offset-black active:scale-[0.98] disabled:opacity-40 disabled:pointer-events-none disabled:active:scale-100 cursor-pointer tracking-tight';

    const variantStyles: Record<ButtonVariant, string> = {
      primary:
        'bg-white text-black hover:bg-white/90 font-semibold shadow-sm active:bg-white/80',
      secondary:
        'bg-white/[0.08] hover:bg-white/[0.14] active:bg-white/[0.18] text-white border border-white/[0.12] shadow-sm backdrop-blur-md',
      destructive:
        'bg-[#FF453A] hover:bg-[#FF453A]/90 active:bg-[#FF453A]/80 text-white font-semibold shadow-sm',
      outline:
        'bg-transparent border border-white/[0.12] hover:border-white/[0.2] hover:bg-white/[0.06] text-white/90 hover:text-white',
      ghost:
        'bg-transparent text-white/70 hover:text-white hover:bg-white/[0.06]',
      subtle:
        'bg-white/[0.04] hover:bg-white/[0.08] text-white/80 hover:text-white border border-white/[0.08]',
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
