package com.adren.travel.whitelabel.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ConsultantRepository extends JpaRepository<Consultant, UUID> {
}
