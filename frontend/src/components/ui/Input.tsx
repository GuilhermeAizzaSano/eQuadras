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
            className="block text-xs font-semibold text-surface-300 tracking-wide select-none"
          >
            {label}
          </label>
        )}
        <div className="relative flex items-center">
          {leftIcon && (
            <div className="absolute left-3.5 text-surface-500 pointer-events-none flex items-center justify-center">
              {leftIcon}
            </div>
          )}
          <input
            id={inputId}
            ref={ref}
            disabled={disabled}
            className={`w-full bg-surface-900/90 border rounded-xl py-2.5 text-xs sm:text-sm text-white placeholder-surface-500 transition-all duration-150 focus:outline-none focus:border-brand-400/80 focus:ring-1 focus:ring-brand-400/40 disabled:opacity-50 disabled:cursor-not-allowed ${
              leftIcon ? 'pl-10' : 'pl-3.5'
            } ${rightElement ? 'pr-11' : 'pr-3.5'} ${
              error
                ? 'border-red-500/80 focus:border-red-500 focus:ring-red-500/40'
                : 'border-surface-800 hover:border-surface-700'
            } ${className}`}
            {...props}
          />
          {rightElement && (
            <div className="absolute right-3 text-surface-400 flex items-center justify-center">
              {rightElement}
            </div>
          )}
        </div>
        {error && <p className="text-[11px] text-red-400 font-medium">{error}</p>}
        {hint && !error && <p className="text-[11px] text-surface-500">{hint}</p>}
      </div>
    );
  }
);

Input.displayName = 'Input';
