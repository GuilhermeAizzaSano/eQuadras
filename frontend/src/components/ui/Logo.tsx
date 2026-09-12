import React from 'react';

export interface LogoProps {
  className?: string;
  size?: number;
  showText?: boolean;
}

export const Logo: React.FC<LogoProps> = ({ className = '', size = 32, showText = true }) => {
  return (
    <div className={`inline-flex items-center gap-3 select-none ${className}`}>
      <div
        style={{ width: size, height: size }}
        className="relative flex items-center justify-center rounded-xl bg-white/[0.06] border border-white/[0.12] shadow-sm shrink-0 overflow-hidden group transition-all"
      >
        <svg
          viewBox="0 0 100 100"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          className="w-[72%] h-[72%] relative z-10 transition-transform duration-300 group-hover:scale-105"
        >
          {/* Perímetro da quadra */}
          <rect
            x="14"
            y="14"
            width="72"
            height="72"
            rx="10"
            stroke="white"
            strokeWidth="6"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          {/* Linhas transversais de demarcação */}
          <line
            x1="14"
            y1="50"
            x2="86"
            y2="50"
            stroke="rgba(255,255,255,0.6)"
            strokeWidth="4"
            strokeLinecap="round"
          />
          {/* Círculo central da quadra */}
          <circle
            cx="50"
            cy="50"
            r="16"
            stroke="white"
            strokeWidth="5"
          />
          {/* Acento minimalista */}
          <path
            d="M 68 76 L 88 88"
            stroke="#0A84FF"
            strokeWidth="7"
            strokeLinecap="round"
          />
        </svg>
      </div>

      {showText && (
        <div className="flex items-baseline tracking-[-0.03em]">
          <span className="text-white/60 text-xl font-medium">e</span>
          <span className="text-white text-xl font-bold">Quadras</span>
        </div>
      )}
    </div>
  );
};

