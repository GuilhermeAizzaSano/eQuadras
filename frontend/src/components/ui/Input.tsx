import React from 'react';

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
  leftIcon?: React.ReactNode;
  rightElement?: React.ReactNode;
}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, hint, leftIcon, rightElement, className = '', id, disabled, ...props }, ref) => {
    const inputId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined);

    return (
      <div className="w-full space-y-1.5">
        {label && (
          <label
            htmlFor={inputId}
            className="block text-xs font-medium text-white/70 tracking-tight select-none"
          >
            {label}
          </label>
        )}
        <div className="relative flex items-center">
          {leftIcon && (
            <div className="absolute left-3.5 text-white/40 pointer-events-none flex items-center justify-center">
              {leftIcon}
            </div>
          )}
          <input
            id={inputId}
            ref={ref}
            disabled={disabled}
            className={`w-full bg-white/[0.04] border rounded-xl py-2.5 text-xs sm:text-sm text-white placeholder-white/30 transition-all duration-150 focus:outline-none focus:border-white/30 focus:ring-2 focus:ring-white/10 shadow-[inset_0_1px_1px_rgba(0,0,0,0.2)] disabled:opacity-40 disabled:cursor-not-allowed ${
              leftIcon ? 'pl-10' : 'pl-3.5'
            } ${rightElement ? 'pr-11' : 'pr-3.5'} ${
              error
                ? 'border-[#FF453A]/80 focus:border-[#FF453A] focus:ring-[#FF453A]/20'
                : 'border-white/[0.08] hover:border-white/[0.15] hover:bg-white/[0.06]'
            } ${className}`}
            {...props}
          />
          {rightElement && (
            <div className="absolute right-3 text-white/40 hover:text-white/80 transition-colors flex items-center justify-center">
              {rightElement}
            </div>
          )}
        </div>
        {error && <p className="text-[11px] text-[#FF453A] font-medium tracking-tight">{error}</p>}
        {hint && !error && <p className="text-[11px] text-white/40 tracking-tight">{hint}</p>}
      </div>
    );
  }
);

Input.displayName = 'Input';
