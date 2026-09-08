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
        className="relative flex items-center justify-center rounded-xl bg-surface-900 border border-surface-750 shadow-md shadow-black/60 shrink-0 overflow-hidden group"
      >
        {/* Glow de fundo sutil */}
        <div className="absolute inset-0 bg-brand-500/10 group-hover:bg-brand-500/20 transition-all duration-300" />
        
        <svg
          viewBox="0 0 100 100"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          className="w-[74%] h-[74%] relative z-10 transition-transform duration-300 group-hover:scale-105"
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
            stroke="rgba(255,255,255,0.7)"
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
          {/* Traço de bola/dinamismo em neon esmeralda */}
          <path
            d="M 68 76 L 88 88"
            stroke="#34d399"
            strokeWidth="8"
            strokeLinecap="round"
          />
        </svg>
      </div>

      {showText && (
        <div className="flex items-baseline font-extrabold tracking-tight">
          <span className="text-brand-400 text-xl font-mono tracking-normal">e</span>
          <span className="text-white text-xl font-sans tracking-tight">Quadras</span>
        </div>
      )}
    </div>
  );
};

