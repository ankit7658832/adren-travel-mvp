import js from "@eslint/js";
import globals from "globals";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";
import jsxA11y from "eslint-plugin-jsx-a11y";
import tseslint from "@typescript-eslint/eslint-plugin";
import tsParser from "@typescript-eslint/parser";

// Flat config (ESLint 9). jsx-a11y only ships an eslintrc-shaped
// `configs.recommended` (string plugin names, not a plugin-object map), so
// its rules are pulled in manually rather than spread as a config object —
// see FND-19 / RULES.md §7.3 reconciliation item #5 ("no ESLint config at
// all despite the dependencies being installed").
export default [
  { ignores: ["dist/**", "coverage/**", "playwright-report/**", "node_modules/**"] },
  js.configs.recommended,
  {
    files: ["**/*.{ts,tsx}"],
    languageOptions: {
      parser: tsParser,
      parserOptions: {
        ecmaFeatures: { jsx: true },
      },
      globals: { ...globals.browser, ...globals.es2021, ...globals.node },
    },
    plugins: {
      "@typescript-eslint": tseslint,
      "react-hooks": reactHooks,
      "react-refresh": reactRefresh,
      "jsx-a11y": jsxA11y,
    },
    rules: {
      ...tseslint.configs.recommended.rules,
      ...reactHooks.configs["recommended-latest"].rules,
      ...jsxA11y.configs.recommended.rules,
      "react-refresh/only-export-components": ["warn", { allowConstantExport: true }],
      "no-unused-vars": "off",
      "@typescript-eslint/no-unused-vars": ["warn", { argsIgnorePattern: "^_" }],
      // Default depth (2) doesn't look far enough into a <label>'s children
      // to find text nested inside two wrapping <span>s (the preset-picker
      // radio pattern in consultant-storefront) — the markup is genuinely
      // accessible (label wraps the input and its visible text), this is a
      // static-analysis depth limitation, not a real violation.
      "jsx-a11y/label-has-associated-control": ["error", { depth: 4 }],
    },
  },
];
