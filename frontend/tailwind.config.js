/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#f0fdf4',
          100: '#dcfce7',
          200: '#bbf7d0',
          300: '#86efac',
          400: '#4ade80',
          500: '#22c55e',
          600: '#16a34a',
          700: '#15803d',
          800: '#166534',
          900: '#14532d',
          950: '#052e16',
        },
        surface: {
          950: '#000000',
          900: '#0c0c0e',
          850: '#121214',
          800: '#1c1c1e',
          750: '#242426',
          700: '#2c2c2e',
          600: '#3a3a3c',
          500: '#48484a',
          400: '#636366',
          300: '#8e8e93',
          200: '#aeaeb2',
          100: '#d1d1d6',
          50: '#f2f2f7',
        },
        apple: {
          blue: '#0A84FF',
          green: '#30D158',
          orange: '#FF9F0A',
          red: '#FF453A',
          yellow: '#FFD60A',
          purple: '#BF5AF2',
          gray: '#8E8E93',
        },
        background: "#000000",
      },
      fontFamily: {
        sans: ['Plus Jakarta Sans', '-apple-system', 'BlinkMacSystemFont', 'SF Pro Text', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'SF Mono', 'ui-monospace', 'monospace'],
      },
      boxShadow: {
        'subtle-border': 'inset 0 1px 0 0 rgba(255, 255, 255, 0.08)',
        'apple-card': '0 2px 8px -2px rgba(0, 0, 0, 0.5), 0 1px 4px -1px rgba(0, 0, 0, 0.3)',
        'apple-elevated': '0 12px 32px -4px rgba(0, 0, 0, 0.6), 0 4px 12px -2px rgba(0, 0, 0, 0.4)',
        'apple-glow': '0 0 20px -5px rgba(255, 255, 255, 0.12)',
      },
    },
  },
  plugins: [],
}
