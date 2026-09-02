import React from 'react';

interface LogoProps {
  className?: string;
  size?: number;
  showText?: boolean;
}

export const Logo: React.FC<LogoProps> = ({ className = '', size = 32, showText = true }) => {
  return (
    <div className={`inline-flex items-center gap-2.5 select-none ${className}`}>
      <div 
        style={{ width: size, height: size }} 
        className="relative flex items-center justify-center rounded-xl bg-zinc-900 border border-zinc-800 shadow-sm shrink-0 overflow-hidden"
      >
        <svg
          viewBox="0 0 100 100"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          className="w-[78%] h-[78%]"
        >
          <rect
            x="14"
            y="14"
            width="72"
            height="72"
            rx="8"
            stroke="white"
            strokeWidth="6"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <line
            x1="14"
            y1="14"
            x2="86"
            y2="86"
            stroke="white"
            strokeWidth="5"
            strokeLinecap="round"
          />
          <line
            x1="14"
            y1="86"
            x2="86"
            y2="14"
            stroke="white"
            strokeWidth="5"
            strokeLinecap="round"
          />
          <circle
            cx="50"
            cy="50"
            r="16"
            stroke="white"
            strokeWidth="5"
          />
          <path
            d="M 66 76 L 88 88"
            stroke="#34d399"
            strokeWidth="8"
            strokeLinecap="round"
          />
        </svg>
      </div>

      {showText && (
        <div className="flex items-baseline font-bold tracking-tight">
          <span className="text-emerald-400 text-lg md:text-xl font-mono">e</span>
          <span className="text-white text-lg md:text-xl font-sans">Quadras</span>
        </div>
      )}
    </div>
  );
};
