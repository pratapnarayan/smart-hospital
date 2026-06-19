/**
 * TODO: Token consolidation (deferred from PR #4 UI revamp)
 *
 * Problem: Design tokens (color palettes, shadow scales, animation keyframes) are currently
 * duplicated across three places:
 *   1. tailwind.config.js  — Tailwind `theme.extend` values
 *   2. src/theme/index.ts  — `designTokens` JS object consumed by components and AppLayout
 *   3. src/index.css       — CSS custom properties (--color-*, --shadow-*, etc.)
 *
 * Any token change (e.g. tweaking the primary-500 blue) must be applied in all three files,
 * which will inevitably drift over time.
 *
 * Proposed fix:
 *   - Create src/tokens/designTokens.ts as the single source of truth (plain JS/TS object).
 *   - Import it into tailwind.config.js using the `theme.extend` spread.
 *   - Import it into src/index.css generation (or use a build step / postcss plugin).
 *   - Replace src/theme/index.ts with a re-export from src/tokens/designTokens.ts.
 *
 * Scope: ~3 files changed, no runtime behaviour change, purely a DX/maintenance improvement.
 * Can be done as a standalone refactor PR before the next feature cycle.
 */

/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  corePlugins: {
    preflight: false,
  },
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#e6f4ff',
          100: '#bae0ff',
          200: '#8cc8ff',
          300: '#5aaaff',
          400: '#338aff',
          500: '#1677ff',
          600: '#0958d9',
          700: '#003eb3',
          800: '#002c8c',
          900: '#001d66',
        },
        secondary: {
          50: '#e6fffb',
          100: '#b5f5ec',
          200: '#87e8de',
          300: '#5cdbd3',
          400: '#36cfc9',
          500: '#13c2c2',
          600: '#08979c',
          700: '#006d75',
          800: '#00474f',
          900: '#002329',
        },
        success: {
          50: '#f6ffed',
          100: '#d9f7be',
          200: '#b7eb8f',
          300: '#95de64',
          400: '#73d13d',
          500: '#52c41a',
          600: '#389e0d',
          700: '#237804',
          800: '#135200',
          900: '#092b00',
        },
        warning: {
          50: '#fffbe6',
          100: '#fff1b8',
          200: '#ffe58f',
          300: '#ffd666',
          400: '#ffc53d',
          500: '#faad14',
          600: '#d48806',
          700: '#ad6800',
          800: '#874d00',
          900: '#613400',
        },
        danger: {
          50: '#fff1f0',
          100: '#ffccc7',
          200: '#ffa39e',
          300: '#ff7875',
          400: '#ff4d4f',
          500: '#f5222d',
          600: '#cf1322',
          700: '#a8071a',
          800: '#820014',
          900: '#5c0011',
        },
        neutral: {
          50: '#f8fafc',
          100: '#f1f5f9',
          200: '#e2e8f0',
          300: '#cbd5e1',
          400: '#94a3b8',
          500: '#64748b',
          600: '#475569',
          700: '#334155',
          800: '#1e293b',
          900: '#0f172a',
          950: '#020617',
        },
        sidebar: {
          bg: '#0f172a',
          hover: '#1e293b',
          active: '#1677ff',
          text: '#94a3b8',
          'text-active': '#ffffff',
          border: '#1e293b',
        },
        background: {
          primary: '#f8fafc',
          secondary: '#f1f5f9',
          tertiary: '#ffffff',
        },
        'text-primary': '#0f172a',
        'text-secondary': '#475569',
        'text-tertiary': '#94a3b8',
        'text-muted': '#64748b',
        chart: {
          primary: '#1677ff',
          secondary: '#13c2c2',
          success: '#52c41a',
          warning: '#faad14',
          danger: '#ff4d4f',
          purple: '#722ed1',
          magenta: '#eb2f96',
          orange: '#fa8c16',
          cyan: '#13c2c2',
          lime: '#a0d911',
        },
      },
      fontFamily: {
        sans: [
          '-apple-system',
          'BlinkMacSystemFont',
          "'Segoe UI'",
          'Roboto',
          "'Helvetica Neue'",
          'Arial',
          'sans-serif',
        ],
        mono: ["'SF Mono'", 'Monaco', "'Cascadia Code'", "'Roboto Mono'", 'monospace'],
      },
      boxShadow: {
        medical: '0 1px 3px 0 rgba(15, 23, 42, 0.1), 0 1px 2px -1px rgba(15, 23, 42, 0.1)',
        'medical-md': '0 4px 6px -1px rgba(15, 23, 42, 0.1), 0 2px 4px -2px rgba(15, 23, 42, 0.1)',
        'medical-lg': '0 10px 15px -3px rgba(15, 23, 42, 0.1), 0 4px 6px -4px rgba(15, 23, 42, 0.1)',
        'medical-xl': '0 20px 25px -5px rgba(15, 23, 42, 0.1), 0 8px 10px -6px rgba(15, 23, 42, 0.1)',
        'medical-2xl': '0 25px 50px -12px rgba(15, 23, 42, 0.25)',
        'glow-primary': '0 0 20px rgba(22, 119, 255, 0.3)',
        'glow-success': '0 0 20px rgba(82, 196, 26, 0.3)',
        'glow-warning': '0 0 20px rgba(250, 173, 20, 0.3)',
        'glow-danger': '0 0 20px rgba(255, 77, 79, 0.3)',
        'glow-purple': '0 0 20px rgba(114, 46, 209, 0.3)',
        'glow-cyan': '0 0 20px rgba(19, 194, 194, 0.3)',
      },
      transitionTimingFunction: {
        medical: 'cubic-bezier(0.4, 0, 0.2, 1)',
      },
      transitionDuration: {
        400: '400ms',
      },
      zIndex: {
        dropdown: '1000',
        sticky: '1020',
        fixed: '1030',
        modal: '1040',
        popover: '1050',
        tooltip: '1060',
      },
      animation: {
        'fade-in': 'fadeIn 0.3s ease-out',
        'slide-up': 'slideUp 0.4s ease-out',
        'scale-in': 'scaleIn 0.2s ease-out',
        shimmer: 'shimmer 1.5s infinite',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%': { opacity: '0', transform: 'translateY(10px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        scaleIn: {
          '0%': { opacity: '0', transform: 'scale(0.95)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
        },
        shimmer: {
          '0%': { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition: '200% 0' },
        },
      },
    },
  },
  plugins: [
    function ({ addUtilities }) {
      addUtilities({
        '.touch-target': {
          minHeight: '44px',
          minWidth: '44px',
        },
        '.text-balance': {
          textWrap: 'balance',
        },
        '.scrollbar-hide': {
          '-ms-overflow-style': 'none',
          'scrollbar-width': 'none',
          '&::-webkit-scrollbar': {
            display: 'none',
          },
        },
      })
    },
  ],
}
