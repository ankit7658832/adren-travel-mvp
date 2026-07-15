package com.adren.travel.payments.internal;

import com.adren.travel.shared.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface MarkupRuleRepository extends JpaRepository<MarkupRule, UUID> {

    Optional<MarkupRule> findByConsultantIdAndCategory(UUID consultantId, ProductCategory category);

    List<MarkupRule> findByConsultantId(UUID consultantId);
}
