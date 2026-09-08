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
          <select
            id={selectId}
            ref={ref}
            disabled={disabled}
            className={`w-full bg-surface-900/90 border rounded-xl py-2.5 text-xs sm:text-sm text-white transition-all duration-150 appearance-none cursor-pointer focus:outline-none focus:border-brand-400/80 focus:ring-1 focus:ring-brand-400/40 disabled:opacity-50 disabled:cursor-not-allowed ${
              leftIcon ? 'pl-10' : 'pl-3.5'
            } pr-10 ${
              error
                ? 'border-red-500/80 focus:border-red-500 focus:ring-red-500/40'
                : 'border-surface-800 hover:border-surface-700'
            } ${className}`}
            {...props}
          >
            {children}
          </select>
          <ChevronDown className="w-4 h-4 text-surface-400 absolute right-3 pointer-events-none" />
        </div>
        {error && <p className="text-[11px] text-red-400 font-medium">{error}</p>}
      </div>
    );
  }
);

Select.displayName = 'Select';
