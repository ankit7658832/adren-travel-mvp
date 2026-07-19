import { create } from "zustand";

export type ToastTone = "success" | "error";

export interface Toast {
  id: string;
  message: string;
  tone: ToastTone;
}

interface ToastQueueStore {
  toasts: Toast[];
  addToast: (toast: Omit<Toast, "id">) => string;
  removeToast: (id: string) => void;
}

/**
 * FES-10 — cross-cutting client state (RULES.md §7.1: outlives any single
 * mutating component, so Zustand, not local `useState`) for platform-wide
 * async-operation feedback (save/publish/payment results) instead of each
 * screen inventing its own transient-message pattern.
 */
export const useToastQueueStore = create<ToastQueueStore>((set) => ({
  toasts: [],

  addToast: (toast) => {
    const id = crypto.randomUUID();
    set((state) => ({ toasts: [...state.toasts, { ...toast, id }] }));
    return id;
  },

  removeToast: (id) => set((state) => ({ toasts: state.toasts.filter((t) => t.id !== id) })),
}));
