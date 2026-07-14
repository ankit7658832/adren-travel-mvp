/**
 * Synthesizes a gradient image client-side, standing in for a Consultant's
 * uploaded background photo, so the storefront demo can exercise the real
 * image-sampling path (sampleImageRegion.ts) without fetching an external
 * image over the network. Browser-only (canvas).
 */
export function generateSampleGradientImage(
  topColor: string,
  bottomColor: string,
  width = 1200,
  height = 500
): string | null {
  const canvas = document.createElement("canvas");
  canvas.width = width;
  canvas.height = height;
  const ctx = canvas.getContext("2d");
  if (!ctx) return null;

  const gradient = ctx.createLinearGradient(0, 0, 0, height);
  gradient.addColorStop(0, topColor);
  gradient.addColorStop(1, bottomColor);
  ctx.fillStyle = gradient;
  ctx.fillRect(0, 0, width, height);

  return canvas.toDataURL("image/png");
}
