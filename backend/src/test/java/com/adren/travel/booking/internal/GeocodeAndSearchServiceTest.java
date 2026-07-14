package com.adren.travel.booking.internal;

import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import com.adren.travel.supplier.SupplierSearchApi;
import com.adren.travel.supplier.SupplierId;
import com.adren.travel.supplier.SupplierSearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeocodeAndSearchServiceTest {

    @Mock
    SupplierSearchApi supplierSearchApi;

    GeocodeAndSearchService service;

    @BeforeEach
    void setUp() {
        service = new GeocodeAndSearchService(new GeocodingService(), supplierSearchApi);
    }

    @Test
    void everyLocationGetsAPinIncludingOneWithNoInventoryT1() {
        LocalDate checkIn = LocalDate.now().plusDays(30);
        LocalDate checkOut = checkIn.plusDays(3);
        when(supplierSearchApi.searchHotels(eq("Goa"), any(), any())).thenReturn(List.of(
            new SupplierSearchResult(SupplierId.HOTELBEDS, "r1", "Hotel A", "Deluxe",
                new Money(BigDecimal.valueOf(5000), CurrencyCode.INR))));
        when(supplierSearchApi.searchHotels(eq("Antarctica"), any(), any())).thenReturn(List.of());

        List<GeocodedLocation> result = service.geocodeAndSearch(List.of("Goa", "Antarctica"), checkIn, checkOut);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).locationCode()).isEqualTo("Goa");
        assertThat(result.get(0).hasInventory()).isTrue();
        assertThat(result.get(1).locationCode()).isEqualTo("Antarctica");
        assertThat(result.get(1).hasInventory()).isFalse();
    }

    @Test
    void everyLocationCarriesGeocodedCoordinates() {
        when(supplierSearchApi.searchHotels(any(), any(), any())).thenReturn(List.of());
        LocalDate checkIn = LocalDate.now().plusDays(30);

        List<GeocodedLocation> result = service.geocodeAndSearch(List.of("Jaipur"), checkIn, checkIn.plusDays(3));

        assertThat(result.get(0).latitude()).isBetween(8.0, 35.0);
        assertThat(result.get(0).longitude()).isBetween(68.0, 97.0);
    }
}
