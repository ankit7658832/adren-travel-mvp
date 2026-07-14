package com.adren.travel.whitelabel.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ConsultantUserRepository extends JpaRepository<ConsultantUser, UUID> {

    Page<ConsultantUser> findByConsultantId(UUID consultantId, Pageable pageable);
}
