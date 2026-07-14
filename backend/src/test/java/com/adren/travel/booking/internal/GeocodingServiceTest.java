package com.adren.travel.booking.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeocodingServiceTest {

    private final GeocodingService service = new GeocodingService();

    @Test
    void isDeterministicForTheSameQuery() {
        var first = service.geocode("Goa");
        var second = service.geocode("Goa");

        assertThat(first).isEqualTo(second);
    }

    @Test
    void isCaseAndWhitespaceInsensitive() {
        assertThat(service.geocode("Goa")).isEqualTo(service.geocode("  goa  "));
    }

    @Test
    void producesDifferentCoordinatesForDifferentLocations() {
        assertThat(service.geocode("Goa")).isNotEqualTo(service.geocode("Jaipur"));
    }

    @Test
    void alwaysReturnsCoordinatesWithinTheDocumentedBoundingBox() {
        for (String query : new String[]{"Goa", "Udaipur", "Jaipur", "", "a-totally-unknown-place-123"}) {
            var point = service.geocode(query);
            assertThat(point.latitude()).isBetween(8.0, 35.0);
            assertThat(point.longitude()).isBetween(68.0, 97.0);
        }
    }
}
