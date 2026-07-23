package com.adren.travel.security.internal;

import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * AUTH-01 — the real, credential-checked login every UI role signs in
 * through (replaces the {@code dev-auth} shortcut for anything other than
 * local development). Public per {@code SecurityConfig.PUBLIC_ENDPOINTS}.
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private final PrincipalCredentialRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordResetEmailSender resetEmailSender;
    private final PasswordResetProperties resetProperties;

    AuthController(PrincipalCredentialRepository repository, PasswordEncoder passwordEncoder,
                    JwtTokenService jwtTokenService, PasswordResetTokenRepository resetTokenRepository,
                    PasswordResetEmailSender resetEmailSender, PasswordResetProperties resetProperties) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.resetTokenRepository = resetTokenRepository;
        this.resetEmailSender = resetEmailSender;
        this.resetProperties = resetProperties;
    }

    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        PrincipalCredential credential = repository.findByEmailIgnoreCase(request.email())
            .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
            .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        AdrenPrincipal principal = new AdrenPrincipal(
            credential.getCredentialId(), credential.getRole(), credential.getConsultantId());
        String token = jwtTokenService.generateToken(principal);
        return new LoginResponse(token, credential.getRole(), credential.getConsultantId());
    }

    /**
     * SCR-00b — always returns 200 whether or not {@code email} matches a
     * real credential; the response never reveals which. If it does
     * match, mints a fresh {@link PasswordResetToken} and dispatches the
     * reset link via {@link PasswordResetEmailSender} (a stub in this
     * mock phase, same "own provider seam" shape as every other external
     * integration in this codebase).
     */
    @PostMapping("/forgot-password")
    void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        repository.findByEmailIgnoreCase(request.email()).ifPresent(credential -> {
            UUID tokenId = UUID.randomUUID();
            Instant expiresAt = Instant.now().plus(Duration.ofMinutes(resetProperties.tokenExpirationMinutes()));
            resetTokenRepository.save(new PasswordResetToken(tokenId, credential.getCredentialId(), expiresAt));
            String resetUrl = resetProperties.frontendBaseUrl() + "/reset-password?token=" + tokenId;
            resetEmailSender.sendResetLink(credential.getEmail(), resetUrl);
        });
    }

    /** SCR-00b — consumes a still-usable (unexpired, unused) token and sets a new password. */
    @PostMapping("/reset-password")
    @Transactional
    void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        PasswordResetToken token = resetTokenRepository.findById(request.token())
            .filter(PasswordResetToken::isUsable)
            .orElseThrow(() -> new IllegalArgumentException("This reset link is invalid or has expired."));
        PrincipalCredential credential = repository.findById(token.getCredentialId())
            .orElseThrow(() -> new IllegalStateException("No credential for token: " + token.getTokenId()));

        credential.changePasswordHash(passwordEncoder.encode(request.newPassword()));
        repository.save(credential);
        token.markUsed();
        resetTokenRepository.save(token);
    }

    record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    record LoginResponse(String token, Role role, UUID consultantId) {
    }

    record ForgotPasswordRequest(@NotBlank @Email String email) {
    }

    record ResetPasswordRequest(@NotNull UUID token, @NotBlank @Size(min = 8) String newPassword) {
    }
}
