import React from 'react';
import { ChevronDown } from 'lucide-react';

export interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  error?: string;
  leftIcon?: React.ReactNode;
}

export const Select = React.forwardRef<HTMLSelectElement, SelectProps>(
  ({ label, error, leftIcon, children, className = '', id, disabled, ...props }, ref) => {
    const selectId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined);

    return (
      <div className="w-full space-y-1.5">
        {label && (
          <label
            htmlFor={selectId}
            className="block text-xs font-medium text-fg/70 tracking-tight select-none"
          >
            {label}
          </label>
        )}
        <div className="relative flex items-center">
          {leftIcon && (
            <div className="absolute left-3.5 text-fg/60 pointer-events-none flex items-center justify-center">
              {leftIcon}
            </div>
          )}
          <select
            id={selectId}
            ref={ref}
            disabled={disabled}
            className={`w-full bg-fg/[0.03] border rounded-xl py-2.5 text-xs sm:text-sm text-fg transition-all duration-150 appearance-none cursor-pointer focus:outline-none focus:border-fg/30 focus:ring-2 focus:ring-fg/10 shadow-[inset_0_1px_1px_rgba(0,0,0,0.2)] disabled:opacity-40 disabled:cursor-not-allowed ${
              leftIcon ? 'pl-10' : 'pl-3.5'
            } pr-10 ${
              error
                ? 'border-danger/80 focus:border-danger focus:ring-danger/20'
                : 'border-fg/[0.1] hover:border-fg/[0.15] hover:bg-fg/[0.06]'
            } ${className}`}
            {...props}
          >
            {children}
          </select>
          <ChevronDown className="w-4 h-4 text-fg/60 absolute right-3 pointer-events-none" />
        </div>
        {error && <p className="text-xs text-danger font-medium tracking-tight">{error}</p>}
      </div>
    );
  }
);

Select.displayName = 'Select';
