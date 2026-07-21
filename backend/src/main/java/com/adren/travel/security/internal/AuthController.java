package com.adren.travel.security.internal;

import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    AuthController(PrincipalCredentialRepository repository, PasswordEncoder passwordEncoder,
                    JwtTokenService jwtTokenService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
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

    record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    record LoginResponse(String token, Role role, UUID consultantId) {
    }
}
