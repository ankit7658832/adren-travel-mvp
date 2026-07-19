package com.adren.travel.ads.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface AdAccountRepository extends JpaRepository<AdAccount, UUID> {

    Optional<AdAccount> findByConsultantId(UUID consultantId);
}
