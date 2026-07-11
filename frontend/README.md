# ADREN TRAVEL — Frontend

React 18 + Vite + TypeScript. Feature-folder structure matching PRD Part 21
(Screen-by-Screen UI Specification) — one folder per screen/feature under
`src/features/`, shared code under `src/shared/`.

## Running locally

```bash
npm install
npm run dev            # http://localhost:5173, proxies /api to backend:8080
```

## Testing

```bash
npm run test           # Vitest — unit + component tests (jsdom, fast)
npm run test:coverage  # same, with coverage thresholds enforced (vite.config.ts)
npm run test:e2e       # Playwright — real browser, full user journeys
```

Test-tier convention: put fast unit/component tests next to the code
(`Component.test.tsx`), reserve `e2e/` for the highest-value journeys per
PRD Section 9.1 (Flow A/B/C). See the `testing-strategy` and
`frontend-react-vite` Claude Code skills before adding new tests or
features.

## Adding a new screen

1. Create `src/features/<screen-name>/`.
2. Check PRD Part 21 for the screen's states (default/loading/empty/error) —
   implement all four, not just the happy path.
3. Add the route in `App.tsx`.
4. Write the component test alongside it before wiring up the real API call.
