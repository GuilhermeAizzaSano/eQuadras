/** @type {import('tailwindcss').Config} */
const withOpacity = (variable) => `rgb(var(${variable}) / <alpha-value>)`;

export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        bg: withOpacity('--bg'),
        fg: withOpacity('--fg'),
        surface: {
          1: withOpacity('--surface-1'),
          2: withOpacity('--surface-2'),
          3: withOpacity('--surface-3'),
        },
        success: withOpacity('--success'),
        danger: withOpacity('--danger'),
        warning: withOpacity('--warning'),
        info: withOpacity('--info'),
        purple: withOpacity('--purple'),
        'on-accent': withOpacity('--on-accent'),
      },
      fontFamily: {
        sans: ['Plus Jakarta Sans', '-apple-system', 'BlinkMacSystemFont', 'SF Pro Text', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'SF Mono', 'ui-monospace', 'monospace'],
      },
      boxShadow: {
        'subtle-border': 'inset 0 1px 0 0 var(--highlight-border)',
        'apple-card': '0 1px 3px 0 rgb(var(--shadow-color) / calc(var(--shadow-strength) * 0.8)), 0 4px 12px -2px rgb(var(--shadow-color) / calc(var(--shadow-strength) * 1.2)), inset 0 1px 0 0 var(--highlight-border)',
        'apple-elevated': '0 8px 30px -4px rgb(var(--shadow-color) / calc(var(--shadow-strength) * 1.8)), 0 4px 12px -2px rgb(var(--shadow-color) / calc(var(--shadow-strength) * 0.9)), inset 0 1px 0 0 var(--highlight-border)',
        'apple-glow': '0 0 24px -4px rgb(var(--fg) / 0.08)',
      },
    },
  },
  plugins: [],
}
