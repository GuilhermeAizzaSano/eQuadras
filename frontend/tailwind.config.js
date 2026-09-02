/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        background: "#09090b",
        surface: "#121215",
        surfaceSubtle: "#18181b",
        border: "#27272a",
        borderFocus: "#52525b",
      }
    },
  },
  plugins: [],
}
