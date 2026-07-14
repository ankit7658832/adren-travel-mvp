package com.adren.travel.whitelabel;

import java.util.UUID;

/** A read projection of a User — never the JPA entity (RULES.md §1.4). */
public record ConsultantUserView(UUID userId, String email, String displayName, boolean canCreatePackage) {
}
