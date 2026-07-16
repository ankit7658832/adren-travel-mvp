package com.adren.travel.supplier.internal.hbactivities;

import com.adren.travel.supplier.SupplierId;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HbActivitiesClientTest {

    private final HbActivitiesClient client = new HbActivitiesClient(WebClient.builder());

    @Test
    void searchReturnsNormalizedResultsTaggedHbActivities() {
        List<HbActivitiesClient.HbActivitiesSearchResult> results = client.search("BOM", LocalDate.now().plusDays(5));

        assertThat(results).isNotEmpty();
        assertThat(results).allSatisfy(result -> assertThat(result.normalized().supplierId()).isEqualTo(SupplierId.HBACTIVITIES));
    }

    @Test
    void searchRepresentsTimeSlotAvailabilityExplicitlyNotAsASingleDayFlag() {
        List<HbActivitiesClient.HbActivitiesSearchResult> results = client.search("BOM", LocalDate.now().plusDays(5));

        List<HbActivitiesClient.ActivityTimeSlot> slots = results.get(0).availableSlots();
        assertThat(slots).hasSizeGreaterThan(1);
        assertThat(slots).anySatisfy(slot -> assertThat(slot.availableCount()).isZero());
        assertThat(slots).anySatisfy(slot -> assertThat(slot.availableCount()).isPositive());
    }
}
