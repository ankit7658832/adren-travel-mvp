package com.adren.travel.security;

import java.util.UUID;

/**
 * Public API of the Security module. Other modules must depend on this
 * interface, never on classes under {@code com.adren.travel.security.internal}.
 */
public interface SecurityApi {

    /**
     * Registers a new login credential and returns its {@code credentialId}
     * (the value that becomes {@link AdrenPrincipal#userId()} on every
     * future login for this identity). Rejects a duplicate email
     * (case-insensitive) with {@link IllegalArgumentException}.
     */
    UUID registerCredential(RegisterCredentialCommand command);
}
