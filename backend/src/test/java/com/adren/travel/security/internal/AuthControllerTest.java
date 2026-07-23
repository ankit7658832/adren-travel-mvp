package com.adren.travel.security.internal;

import com.adren.travel.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AUTH-01/SCR-00b — login, forgot-password, and reset-password, MockMvc-standalone (RULES.md §3.3's testing tier). */
class AuthControllerTest {

    private static final String RAW_PASSWORD = "InitialPassword1!";

    private final PrincipalCredentialRepository repository = mock(PrincipalCredentialRepository.class);
    private final PasswordResetTokenRepository resetTokenRepository = mock(PasswordResetTokenRepository.class);
    private final PasswordResetEmailSender resetEmailSender = mock(PasswordResetEmailSender.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtTokenService jwtTokenService = new JwtTokenService(
        new JwtProperties("test-only-secret-at-least-48-bytes-long-for-hs384!!", 60));
    private final PasswordResetProperties resetProperties =
        new PasswordResetProperties("http://localhost:5173", 30);

    private MockMvc mockMvc;
    private PrincipalCredential credential;

    @BeforeEach
    void setUp() {
        credential = new PrincipalCredential(UUID.randomUUID(), "owner@testco.example",
            passwordEncoder.encode(RAW_PASSWORD), Role.CONSULTANT, UUID.randomUUID());
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AuthController(repository, passwordEncoder, jwtTokenService, resetTokenRepository,
                    resetEmailSender, resetProperties))
            .setControllerAdvice(new SecurityControllerAdvice())
            .build();
    }

    @Test
    void loginWithCorrectCredentialsReturnsAValidToken() throws Exception {
        when(repository.findByEmailIgnoreCase("owner@testco.example")).thenReturn(Optional.of(credential));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content("{\"email\": \"owner@testco.example\", \"password\": \"" + RAW_PASSWORD + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("CONSULTANT"))
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginWithWrongPasswordIsRejectedWithoutRevealingWhichFieldWasWrong() throws Exception {
        when(repository.findByEmailIgnoreCase("owner@testco.example")).thenReturn(Optional.of(credential));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content("{\"email\": \"owner@testco.example\", \"password\": \"wrong-password\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("Invalid email or password."));
    }

    @Test
    void loginWithAnUnknownEmailIsRejectedTheSameWayAsAWrongPassword() throws Exception {
        when(repository.findByEmailIgnoreCase("nobody@testco.example")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content("{\"email\": \"nobody@testco.example\", \"password\": \"" + RAW_PASSWORD + "\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("Invalid email or password."));
    }

    @Test
    void forgotPasswordForAKnownEmailIssuesATokenAndSendsTheResetLink() throws Exception {
        when(repository.findByEmailIgnoreCase("owner@testco.example")).thenReturn(Optional.of(credential));

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType("application/json")
                .content("{\"email\": \"owner@testco.example\"}"))
            .andExpect(status().isOk());

        verify(resetEmailSender).sendResetLink(org.mockito.ArgumentMatchers.eq("owner@testco.example"),
            org.mockito.ArgumentMatchers.contains("http://localhost:5173/reset-password?token="));
    }

    @Test
    void forgotPasswordForAnUnknownEmailStillReturns200WithoutSendingAnything() throws Exception {
        when(repository.findByEmailIgnoreCase("nobody@testco.example")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType("application/json")
                .content("{\"email\": \"nobody@testco.example\"}"))
            .andExpect(status().isOk());

        verify(resetEmailSender, never()).sendResetLink(any(), any());
    }

    @Test
    void resetPasswordWithAValidTokenChangesThePasswordAndConsumesTheToken() throws Exception {
        UUID tokenId = UUID.randomUUID();
        PasswordResetToken token = new PasswordResetToken(tokenId, credential.getCredentialId(),
            Instant.now().plus(30, ChronoUnit.MINUTES));
        when(resetTokenRepository.findById(tokenId)).thenReturn(Optional.of(token));
        when(repository.findById(credential.getCredentialId())).thenReturn(Optional.of(credential));

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType("application/json")
                .content("{\"token\": \"" + tokenId + "\", \"newPassword\": \"NewPassword1!\"}"))
            .andExpect(status().isOk());

        verify(repository).save(credential);
        verify(resetTokenRepository).save(token);
    }

    @Test
    void resetPasswordWithAnExpiredTokenIsRejected() throws Exception {
        UUID tokenId = UUID.randomUUID();
        PasswordResetToken expiredToken = new PasswordResetToken(tokenId, credential.getCredentialId(),
            Instant.now().minus(1, ChronoUnit.MINUTES));
        when(resetTokenRepository.findById(tokenId)).thenReturn(Optional.of(expiredToken));

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType("application/json")
                .content("{\"token\": \"" + tokenId + "\", \"newPassword\": \"NewPassword1!\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void resetPasswordWithAnUnknownTokenIsRejected() throws Exception {
        UUID tokenId = UUID.randomUUID();
        when(resetTokenRepository.findById(tokenId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType("application/json")
                .content("{\"token\": \"" + tokenId + "\", \"newPassword\": \"NewPassword1!\"}"))
            .andExpect(status().isBadRequest());
    }
}
