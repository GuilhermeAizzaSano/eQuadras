/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        zinc: {
          950: '#0f141c', // Fundo principal grafite elegante (não preto breu)
          900: '#18202c', // Cartões e blocos em relevo com destaque
          850: '#212a3b', // Superfícies intermediárias e cabeçalhos
          800: '#2d3748', // Bordas nítidas e destacadas
          750: '#38455a',
          700: '#4a5b75', // Bordas de foco e divisores
          600: '#64748b',
          500: '#94a3b8',
          400: '#cbd5e1', // Texto secundário com excelente contraste
          300: '#e2e8f0', // Texto de formulários e labels
          200: '#f1f5f9', // Títulos destacados
          100: '#f8fafc',
          50: '#ffffff'
        },
        background: "#0f141c",
        surface: "#18202c",
        surfaceSubtle: "#212a3b",
        border: "#2d3748",
        borderFocus: "#38bdf8",
      }
    },
  },
  plugins: [],
}
