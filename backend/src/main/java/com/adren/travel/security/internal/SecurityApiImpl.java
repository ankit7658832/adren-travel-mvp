package com.adren.travel.security.internal;

import com.adren.travel.security.RegisterCredentialCommand;
import com.adren.travel.security.Role;
import com.adren.travel.security.SecurityApi;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
class SecurityApiImpl implements SecurityApi {

    private final PrincipalCredentialRepository repository;
    private final PasswordEncoder passwordEncoder;

    SecurityApiImpl(PrincipalCredentialRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UUID registerCredential(RegisterCredentialCommand command) {
        if (command.role() == Role.SUPER_ADMIN && command.consultantId() != null) {
            throw new IllegalArgumentException("SUPER_ADMIN credentials must not carry a consultantId");
        }
        if (command.role() != Role.SUPER_ADMIN && command.consultantId() == null) {
            throw new IllegalArgumentException("consultantId is required for role " + command.role());
        }
        if (repository.existsByEmailIgnoreCase(command.email())) {
            throw new IllegalArgumentException("A credential already exists for email: " + command.email());
        }

        String passwordHash = passwordEncoder.encode(command.rawPassword());
        PrincipalCredential credential = new PrincipalCredential(
            command.principalUserId(), command.email(), passwordHash, command.role(), command.consultantId());
        repository.save(credential);
        return command.principalUserId();
    }
}
