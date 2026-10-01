/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#1d4ed8',
          dark: '#0b1f4d',
          tint: 'rgba(29, 78, 216, 0.09)',
        },
        secondary: {
          DEFAULT: '#f97316',
          tint: 'rgba(249, 115, 22, 0.13)',
        },
        accent: {
          DEFAULT: '#dc2626',
          tint: 'rgba(220, 38, 38, 0.1)',
        },
        ink: {
          DEFAULT: '#121212',
          70: 'rgba(18, 18, 18, 0.72)',
          50: 'rgba(18, 18, 18, 0.5)',
          30: 'rgba(18, 18, 18, 0.28)',
        },
        line: 'rgba(18, 18, 18, 0.12)',
        bgsoft: '#f5f5f3',
      },
      fontFamily: {
        sans: ['Inter', 'Segoe UI', 'sans-serif'],
        mono: ['IBM Plex Mono', 'monospace'],
      },
    },
  },
  plugins: [],
}