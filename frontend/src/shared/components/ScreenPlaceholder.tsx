interface ScreenPlaceholderProps {
  /** PRD Part 21 screen title, e.g. "21.5 Consultant Dashboard". */
  title: string;
  /** The story that builds this screen for real, e.g. "HRD-09". */
  builtBy: string;
}

/**
 * Placeholder body for a PRD Part 21 screen that's registered as a route
 * (FES-01) before the story that builds its real implementation lands —
 * lets the route/code-splitting/navigation shape exist up front rather than
 * being retrofitted once every screen's feature work is done.
 */
export function ScreenPlaceholder({ title, builtBy }: ScreenPlaceholderProps) {
  return (
    <main className="mx-auto max-w-4xl px-6 py-8">
      <h1 className="text-2xl font-semibold text-neutral-900">{title}</h1>
      <p className="mt-2 text-sm text-neutral-600">
        Not yet implemented — see story {builtBy}.
      </p>
    </main>
  );
}
