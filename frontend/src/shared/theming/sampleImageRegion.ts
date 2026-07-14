/**
 * doc/DESIGN.md §3.3 step 1 — turns an uploaded background image into a
 * flat "effective background color" for a given region (e.g. the header
 * band, or the hero-text safe zone), so the pure contrastSafety.ts
 * functions have a single color to reason about instead of a bitmap.
 *
 * This is deliberately kept out of contrastSafety.ts: it's DOM/canvas
 * dependent (not testable as a pure function the way the math is), and
 * doc/DESIGN.md §3.3 treats "sample the image" and "compute contrast for a
 * flat color" as two separate steps for exactly this reason.
 */

export interface SampleRegion {
  /** Fractional bounds within the image, 0–1. Defaults to the whole image. */
  xStart?: number;
  xEnd?: number;
  yStart?: number;
  yEnd?: number;
}

/** doc/DESIGN.md §3.3 step 1 — header band is the top ~15% of the image. */
export const HEADER_BAND_REGION: SampleRegion = { yStart: 0, yEnd: 0.15 };

/** doc/DESIGN.md §3.4 — hero text safe zone, roughly centered. */
export const HERO_SAFE_ZONE_REGION: SampleRegion = {
  xStart: 0.1,
  xEnd: 0.9,
  yStart: 0.35,
  yEnd: 0.75,
};

function toHex(n: number): string {
  return Math.round(Math.min(255, Math.max(0, n)))
    .toString(16)
    .padStart(2, "0");
}

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = "anonymous";
    img.onload = () => resolve(img);
    img.onerror = () =>
      reject(new Error(`Failed to load image for sampling: ${src}`));
    img.src = src;
  });
}

/**
 * Downsamples the given region of an image to a small canvas and returns
 * the average color as a hex string. Runs client-side, at branding-save
 * time (doc/DESIGN.md §3.3 step 1) — not on every page render.
 */
export async function sampleAverageColor(
  imageSrc: string,
  region: SampleRegion = {}
): Promise<string> {
  const { xStart = 0, xEnd = 1, yStart = 0, yEnd = 1 } = region;
  const img = await loadImage(imageSrc);

  const sampleSize = 16; // small on purpose — this only needs an average, not detail
  const canvas = document.createElement("canvas");
  canvas.width = sampleSize;
  canvas.height = sampleSize;
  const ctx = canvas.getContext("2d");
  if (!ctx) {
    throw new Error("Canvas 2D context unavailable — cannot sample image");
  }

  const sx = xStart * img.naturalWidth;
  const sy = yStart * img.naturalHeight;
  const sWidth = (xEnd - xStart) * img.naturalWidth;
  const sHeight = (yEnd - yStart) * img.naturalHeight;

  ctx.drawImage(img, sx, sy, sWidth, sHeight, 0, 0, sampleSize, sampleSize);
  const { data } = ctx.getImageData(0, 0, sampleSize, sampleSize);

  let r = 0;
  let g = 0;
  let b = 0;
  const pixelCount = data.length / 4;
  for (let i = 0; i < data.length; i += 4) {
    r += data[i];
    g += data[i + 1];
    b += data[i + 2];
  }

  return `#${toHex(r / pixelCount)}${toHex(g / pixelCount)}${toHex(b / pixelCount)}`;
}
