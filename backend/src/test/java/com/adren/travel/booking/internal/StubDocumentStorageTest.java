package com.adren.travel.booking.internal;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class StubDocumentStorageTest {

    private final StubDocumentStorage storage = new StubDocumentStorage();

    @Test
    void returnsAReferenceUnderTheGivenKeyPrefix() {
        String reference = storage.store("vouchers/some-booking-id", "content".getBytes(StandardCharsets.UTF_8));

        assertThat(reference).startsWith("vouchers/some-booking-id/").endsWith(".pdf");
    }

    @Test
    void returnsADifferentReferenceOnEachCall() {
        String first = storage.store("vouchers/booking", "a".getBytes(StandardCharsets.UTF_8));
        String second = storage.store("vouchers/booking", "b".getBytes(StandardCharsets.UTF_8));

        assertThat(first).isNotEqualTo(second);
    }
}
