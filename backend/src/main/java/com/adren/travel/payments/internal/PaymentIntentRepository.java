package com.adren.travel.payments.internal;

import org.springframework.data.jpa.repository.JpaRepository;

interface PaymentIntentRepository extends JpaRepository<PaymentIntentRecord, String> {
}
