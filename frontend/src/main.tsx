import React from "react";
import ReactDOM from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import { AppProviders } from "./shared/providers/AppProviders";
import "./index.css";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30_000,
    },
  },
});

// FES-02: app-wide context providers (auth, ...) live in AppProviders,
// slotted here between QueryClientProvider and BrowserRouter — see
// src/shared/providers/AppProviders.tsx. Theme/branding state is a
// Zustand store (src/shared/theming/tenantThemeStore.ts, per
// doc/architecture/RULES.md §7.1) and doesn't need a slot here.
ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <AppProviders>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </AppProviders>
    </QueryClientProvider>
  </React.StrictMode>
);
