package com.adren.travel.whitelabel.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A Consultant's storefront branding (PRD §13.2, doc/DESIGN.md §3.1) —
 * package-private, package-owned table (RULES.md §4.2). {@code consultantId}
 * is both the primary key and the FK: exactly one branding profile per
 * Consultant. Only the four raw tenant-input fields plus {@code domain} are
 * stored here — the resolved/scrim tokens doc/DESIGN.md §3.3 step 5
 * describes are computed and cached client-side (see
 * {@code frontend/src/shared/theming/resolveTenantTheme.ts}); persisting
 * them server-side is FND-07's concern (propagation without redeploy), not
 * this story's.
 */
@Entity
@Table(name = "branding_profile")
class BrandingProfile {

    @Id
    private UUID consultantId;

    private String logoUrl;

    private String backgroundImageUrl;

    private String backgroundColor;

    private String textColorPrimary;

    private String textColorSecondary;

    private String domain;

    private Instant updatedAt;

    protected BrandingProfile() {
        // JPA
    }

    BrandingProfile(UUID consultantId, String logoUrl, String backgroundImageUrl, String backgroundColor,
                     String textColorPrimary, String textColorSecondary, String domain) {
        this.consultantId = consultantId;
        this.logoUrl = logoUrl;
        this.backgroundImageUrl = backgroundImageUrl;
        this.backgroundColor = backgroundColor;
        this.textColorPrimary = textColorPrimary;
        this.textColorSecondary = textColorSecondary;
        this.domain = domain;
        this.updatedAt = Instant.now();
    }

    void update(String logoUrl, String backgroundImageUrl, String backgroundColor,
                String textColorPrimary, String textColorSecondary, String domain) {
        this.logoUrl = logoUrl;
        this.backgroundImageUrl = backgroundImageUrl;
        this.backgroundColor = backgroundColor;
        this.textColorPrimary = textColorPrimary;
        this.textColorSecondary = textColorSecondary;
        this.domain = domain;
        this.updatedAt = Instant.now();
    }

    UUID getConsultantId() {
        return consultantId;
    }

    String getLogoUrl() {
        return logoUrl;
    }

    String getBackgroundImageUrl() {
        return backgroundImageUrl;
    }

    String getBackgroundColor() {
        return backgroundColor;
    }

    String getTextColorPrimary() {
        return textColorPrimary;
    }

    String getTextColorSecondary() {
        return textColorSecondary;
    }

    String getDomain() {
        return domain;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}
