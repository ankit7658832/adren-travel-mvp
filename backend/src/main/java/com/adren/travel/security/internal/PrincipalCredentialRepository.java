package com.adren.travel.security.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface PrincipalCredentialRepository extends JpaRepository<PrincipalCredential, UUID> {

    Optional<PrincipalCredential> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
