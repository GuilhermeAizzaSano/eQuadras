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
            className="block text-xs font-medium text-fg/70 tracking-tight select-none"
          >
            {label}
          </label>
        )}
        <div className="relative flex items-center">
          {leftIcon && (
            <div className="absolute left-3.5 text-fg/40 pointer-events-none flex items-center justify-center">
              {leftIcon}
            </div>
          )}
          <input
            id={inputId}
            ref={ref}
            disabled={disabled}
            className={`w-full bg-fg/[0.03] border rounded-xl py-2.5 text-xs sm:text-sm text-fg placeholder-fg/30 transition-all duration-150 focus:outline-none focus:border-fg/40 focus:bg-fg/[0.06] disabled:opacity-40 disabled:cursor-not-allowed ${
              leftIcon ? 'pl-10' : 'pl-3.5'
            } ${rightElement ? 'pr-11' : 'pr-3.5'} ${
              error
                ? 'border-danger/80 focus:border-danger'
                : 'border-fg/[0.1] hover:border-fg/[0.15]'
            } ${className}`}
            {...props}
          />
          {rightElement && (
            <div className="absolute right-3 text-fg/40 hover:text-fg/80 transition-colors flex items-center justify-center">
              {rightElement}
            </div>
          )}
        </div>
        {error && <p className="text-[11px] text-danger font-medium tracking-tight">{error}</p>}
        {hint && !error && <p className="text-[11px] text-fg/40 tracking-tight">{hint}</p>}
      </div>
    );
  }
);

Input.displayName = 'Input';
