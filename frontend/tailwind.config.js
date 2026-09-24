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
        'subtle-border': 'inset 0 1px 0 0 rgb(var(--fg) / 0.08)',
        'apple-card': '0 2px 8px -2px rgb(var(--bg) / 0.5), 0 1px 4px -1px rgb(var(--bg) / 0.3)',
        'apple-elevated': '0 12px 32px -4px rgb(var(--bg) / 0.6), 0 4px 12px -2px rgb(var(--bg) / 0.4)',
        'apple-glow': '0 0 20px -5px rgb(var(--fg) / 0.12)',
      },
    },
  },
  plugins: [],
}
