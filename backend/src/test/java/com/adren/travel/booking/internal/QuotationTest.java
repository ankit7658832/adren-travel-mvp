package com.adren.travel.booking.internal;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuotationTest {

    @Test
    void recalculateUpdatesTravelerCountAndResetsValidUntilBOK18() {
        Quotation quotation = new Quotation(UUID.randomUUID(), UUID.randomUUID(), Instant.now().minusSeconds(1));
        Instant freshValidUntil = Instant.now().plusSeconds(3600);

        quotation.recalculate(4, freshValidUntil);

        assertThat(quotation.getTravelerCount()).isEqualTo(4);
        assertThat(quotation.getValidUntil()).isEqualTo(freshValidUntil);
    }

    @Test
    void recalculateRejectsANonPositiveTravelerCountBOK18() {
        Quotation quotation = new Quotation(UUID.randomUUID(), UUID.randomUUID(), Instant.now());

        assertThatThrownBy(() -> quotation.recalculate(0, Instant.now()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void newQuotationDefaultsToOneTraveler() {
        Quotation quotation = new Quotation(UUID.randomUUID(), UUID.randomUUID(), Instant.now());

        assertThat(quotation.getTravelerCount()).isEqualTo(1);
    }
}
