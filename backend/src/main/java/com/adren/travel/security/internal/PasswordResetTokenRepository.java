package com.adren.travel.security.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
}
