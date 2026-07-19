import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/shared/api/apiClient";
import type { BrandingProfile } from "./resolveTenantTheme";

interface BrandingProfileDto {
  consultantId: string;
  logoUrl: string | null;
  backgroundImageUrl: string | null;
  backgroundColor: string;
  textColorPrimary: string;
  textColorSecondary: string;
  domain: string | null;
  updatedAt: string;
}

/**
 * FES-06 — the runtime theme provider: fetches a Consultant's real
 * branding profile (FND-06/FND-07, `GET /consultants/{id}/branding`) so a
 * storefront's CSS custom properties (via `resolveTenantTheme` +
 * `TenantThemedSurface`) are set from live data at runtime, never a
 * build-time-baked Tailwind value. `BrandingProfileView` carries no
 * display-name field — `domain` is the closest real identity string the
 * profile itself returns, used here as `consultantName` until a dedicated
 * single-consultant lookup exists.
 */
export function useTenantBranding(consultantId: string) {
  return useQuery({
    queryKey: ["tenant-branding", consultantId],
    queryFn: async () => {
      const { data } = await apiClient.get<BrandingProfileDto>(`/consultants/${consultantId}/branding`);
      const profile: BrandingProfile = {
        consultantName: data.domain ?? "Your storefront",
        logoUrl: data.logoUrl,
        backgroundImageUrl: data.backgroundImageUrl,
        backgroundColor: data.backgroundColor,
        textColorPrimary: data.textColorPrimary,
        textColorSecondary: data.textColorSecondary,
      };
      return profile;
    },
    enabled: Boolean(consultantId),
  });
}
