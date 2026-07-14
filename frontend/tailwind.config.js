/**
 * Layer 1 static Tailwind config — doc/DESIGN.md §6, §13.
 *
 * Colors resolve through CSS custom properties (src/shared/design-system/tokens.css),
 * not literal hex, so the same utility classes work for Layer 1 chrome (tokens
 * fixed at :root) without special-casing. Layer 2 surfaces use a separate,
 * runtime-injected variable namespace (--tenant-*, see src/shared/theming/)
 * and are NOT part of this config — see doc/DESIGN.md §3.
 *
 * Tailwind's default spacing scale (1=4px, 2=8px, 4=16px, 6=24px, 8=32px,
 * 10=40px, 12=48px, 16=64px, 20=80px) already matches doc/DESIGN.md §5's
 * 4px spacing scale exactly, so it is intentionally left un-overridden.
 */

/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        primary: {
          50: "var(--color-primary-50)",
          600: "var(--color-primary-600)",
          700: "var(--color-primary-700)",
          DEFAULT: "var(--color-primary-600)",
        },
        secondary: {
          700: "var(--color-secondary-700)",
          900: "var(--color-secondary-900)",
          DEFAULT: "var(--color-secondary-900)",
        },
        success: {
          50: "var(--color-success-50)",
          500: "var(--color-success-500)",
          700: "var(--color-success-700)",
        },
        warning: {
          50: "var(--color-warning-50)",
          500: "var(--color-warning-500)",
          700: "var(--color-warning-700)",
        },
        info: {
          50: "var(--color-info-50)",
          600: "var(--color-info-600)",
          700: "var(--color-info-700)",
        },
        error: {
          50: "var(--color-error-50)",
          600: "var(--color-error-600)",
          700: "var(--color-error-700)",
        },
        neutral: {
          50: "var(--color-neutral-50)",
          100: "var(--color-neutral-100)",
          200: "var(--color-neutral-200)",
          300: "var(--color-neutral-300)",
          400: "var(--color-neutral-400)",
          500: "var(--color-neutral-500)",
          600: "var(--color-neutral-600)",
          700: "var(--color-neutral-700)",
          900: "var(--color-neutral-900)",
        },
        surface: "var(--color-surface)",
        "focus-ring": "var(--color-focus-ring)",
      },
      fontFamily: {
        sans: "var(--font-sans)",
      },
      fontSize: {
        xs: ["12px", "16px"],
        sm: ["14px", "20px"],
        base: ["16px", "24px"],
        lg: ["18px", "28px"],
        xl: ["20px", "28px"],
        "2xl": ["24px", "32px"],
        "3xl": ["30px", "36px"],
        "4xl": ["36px", "44px"],
      },
      transitionDuration: {
        micro: "120ms",
        standard: "200ms",
        page: "250ms",
      },
      // Layer 2 tenant tokens — see doc/DESIGN.md §3.6, src/shared/theming/.
      // Only components under src/shared/theming/ and src/features/consultant-storefront/
      // may reference these. Layer 1 components must never use them.
      backgroundImage: {
        "tenant-bg": "var(--tenant-bg-image)",
      },
    },
  },
  plugins: [],
};
