import type { Config } from 'tailwindcss';

const config: Config = {
  darkMode: 'class',
  content: ['./src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // Taken from the school logo: deep navy and warm gold.
        navy: {
          50: '#eef2fb',
          100: '#d9e2f6',
          200: '#b3c5ed',
          300: '#7f9ade',
          400: '#4f70c6',
          500: '#2f52a8',
          600: '#1f3d86',
          700: '#162f6b',
          800: '#102254',
          900: '#0b1a42',
          950: '#071230',
        },
        gold: {
          50: '#fff9e0',
          100: '#fff0b3',
          200: '#fde47a',
          300: '#f9d23f',
          400: '#f5c21a',
          500: '#f2b705',
          600: '#d19a02',
          700: '#a37600',
          800: '#7a5800',
          900: '#4d3700',
        },
      },
      fontFamily: {
        sans: ['"Segoe UI"', 'Roboto', 'Inter', 'system-ui', '"Noto Sans Devanagari"', '"Nirmala UI"', 'sans-serif'],
      },
      boxShadow: {
        soft: '0 10px 30px -12px rgba(11, 26, 66, 0.25)',
      },
    },
  },
  plugins: [],
};

export default config;
