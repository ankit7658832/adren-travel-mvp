package com.adren.travel.supplier;

import java.util.UUID;

/**
 * Thrown when a reviewer attempts to activate a Local DMC without recording
 * at least one verification step (PRD §10.3 steps 2-3, §22.5 T9, DMC-02) —
 * a normal, anticipated business-rule block, not an unexpected failure, so
 * it's mapped to 409 rather than surfacing as a generic error.
 */
public class LocalDmcVerificationRequiredException extends RuntimeException {

    public LocalDmcVerificationRequiredException(UUID localDmcId) {
        super("Local DMC " + localDmcId + " cannot be activated — at least one verification step "
            + "(non-blank verification notes) must be recorded first (PRD §10.3)");
    }
}
