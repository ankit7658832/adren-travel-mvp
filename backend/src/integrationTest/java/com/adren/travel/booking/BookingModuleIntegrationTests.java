package com.adren.travel.booking;

import com.adren.travel.booking.event.BookingConfirmedEvent;
import com.adren.travel.booking.event.HotelLineItemAddedEvent;
import com.adren.travel.booking.event.ItineraryQuotationSavedEvent;
import com.adren.travel.booking.event.PackageCreatedEvent;
import com.adren.travel.booking.event.PackagePublishedEvent;
import com.adren.travel.booking.event.TravelerProfileCreatedEvent;
import com.adren.travel.payments.ConfigureMarkupCommand;
import com.adren.travel.payments.MarkupType;
import com.adren.travel.payments.PaymentsApi;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import com.adren.travel.shared.ProductCategory;
import com.adren.travel.supplier.SupplierId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code @ApplicationModuleTest} boots ONLY the Booking module (plus the
 * shared/open modules it depends on) — not the whole application context.
 * This is the Spring Modulith equivalent of a "slice test": faster than
 * {@code @SpringBootTest}, but still exercises real Spring wiring and a
 * real (embedded/test) database, unlike a pure Mockito unit test.
 * <p>
 * {@link Scenario} lets you assert on published events without manually
 * wiring a test listener — this is the standard way to verify the
 * event-driven contract PRD Section 15 depends on.
 * <p>
 * {@code DIRECT_DEPENDENCIES} (rather than the default {@code STANDALONE},
 * which boots only this module) is required since FND-13:
 * {@code GeocodeAndSearchService} has a real constructor dependency on
 * {@code supplier.SupplierSearchApi}. {@code extraIncludes = "security"}
 * is required since FND-09/FND-05: {@code whitelabel.internal.WhitelabelServiceImpl}
 * (itself pulled in because {@code booking} directly depends on
 * {@code whitelabel.WhitelabelApi}) has its own constructor dependency on
 * {@code security.CapabilityGrantService} — DIRECT_DEPENDENCIES only
 * widens one hop from the tested module ({@code booking}'s own direct
 * dependencies), not transitively through a dependency's dependencies, so
 * this second-degree edge has to be named explicitly.
 */
@ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES, extraIncludes = "security")
class BookingModuleIntegrationTests {

    /**
     * {@code @ApplicationModuleTest}'s slice doesn't auto-configure
     * {@code WebClient.Builder} the way a full {@code @SpringBootTest}
     * would — {@code supplier}'s {@code HotelbedsClient} (now reachable
     * transitively via FND-13's {@code GeocodeAndSearchService}) needs one.
     */
    @TestConfiguration
    static class WebClientTestConfig {
        @Bean
        WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }
    }

    @Autowired
    BookingApi bookingApi;

    // Field injection is fine for same-module/framework types (above/below),
    // but Spring Modulith's own architecture check flags field injection of
    // ANOTHER application module's type from outside it — constructor
    // injection makes this test's real cross-module dependency (on
    // payments, exercised by BOK-03's tests below) visible up front.
    final PaymentsApi paymentsApi;

    @Autowired
    BookingModuleIntegrationTests(PaymentsApi paymentsApi) {
        this.paymentsApi = paymentsApi;
    }

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void confirmingABookingPublishesBookingConfirmedEvent(Scenario scenario) {
        UUID packageId = UUID.randomUUID();
        Money price = new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR);
        // FND-05's tenant-active gate is exercised in BookingServiceImplTest;
        // authenticate as SUPER_ADMIN here (no consultantId, gate skipped)
        // so this test stays focused on the event-publication contract.
        authenticateAsSuperAdmin();

        scenario.stimulate(() -> bookingApi.confirmBooking(packageId, price))
            .andWaitForEventOfType(BookingConfirmedEvent.class)
            .matchingMappedValue(BookingConfirmedEvent::totalSellPrice, price);
    }

    /**
     * BOK-01's actual acceptance criterion: {@code confirmBooking}'s
     * {@code @Transactional} boundary means the JPA event publication
     * registry write is part of the SAME physical transaction as
     * {@code confirmBooking}'s own work, not a separate one that could
     * commit independently. {@code TransactionTemplate} with default
     * (REQUIRED) propagation makes {@code confirmBooking} join this test's
     * outer transaction rather than open its own — so forcing the outer
     * transaction to roll back after {@code confirmBooking} returns proves
     * the registry row rolls back with it, not just that the Java call
     * sequence happened to throw before reaching the publish line.
     */
    @Test
    void confirmBookingsEventPublicationRegistryEntryRollsBackWithItsSurroundingTransactionBOK01() {
        authenticateAsSuperAdmin();
        Money price = new Money(BigDecimal.valueOf(87_654.32), CurrencyCode.AED);
        AtomicReference<UUID> bookingIdRef = new AtomicReference<>();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            bookingIdRef.set(bookingApi.confirmBooking(UUID.randomUUID(), price));
            throw new IllegalStateException("BOK-01: forcing a rollback after confirmBooking to prove outbox atomicity");
        })).isInstanceOf(IllegalStateException.class);

        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM event_publication WHERE serialized_event LIKE ?",
            Long.class, "%" + bookingIdRef.get() + "%");
        assertThat(count).isZero();
    }

    @Test
    void creatingATravelerProfilePublishesTravelerProfileCreatedEvent(Scenario scenario) {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        var command = new CreateTravelerProfileCommand("Jane Traveler", LocalDate.of(1990, 5, 1),
            "P1234567", LocalDate.of(2030, 1, 1), "IN", List.of(), Map.of());

        scenario.stimulate(() -> bookingApi.createTravelerProfile(command))
            .andWaitForEventOfType(TravelerProfileCreatedEvent.class)
            .matchingMappedValue(TravelerProfileCreatedEvent::consultantId, consultantId);
    }

    @Test
    void addingAHotelLineItemPublishesHotelLineItemAddedEventBOK03(Scenario scenario) {
        UUID consultantId = UUID.randomUUID();
        // FND-05's tenant-active gate does a real lookup against
        // whitelabel's consultant table — authenticate as SUPER_ADMIN
        // (gate skipped) so this test stays focused on the
        // event-publication contract, same as confirmBooking's test above,
        // rather than needing a fixture Consultant row too.
        authenticateAsSuperAdmin();
        UUID itineraryId = insertDraftItinerary(consultantId);
        paymentsApi.configureMarkup(consultantId, new ConfigureMarkupCommand(
            ProductCategory.HOTEL, MarkupType.PERCENTAGE, BigDecimal.valueOf(15), null, null));

        var command = new AddHotelLineItemCommand(SupplierId.HOTELBEDS, "rate-key-1", "Taj Palace", "Deluxe Room",
            MealPlan.BB, Instant.now().plusSeconds(3600), new Money(BigDecimal.valueOf(100), CurrencyCode.INR),
            CurrencyCode.INR, BigDecimal.valueOf(96), BigDecimal.valueOf(3), BigDecimal.ZERO);

        scenario.stimulate(() -> bookingApi.addHotelLineItem(itineraryId, command))
            .andWaitForEventOfType(HotelLineItemAddedEvent.class)
            .matchingMappedValue(HotelLineItemAddedEvent::itineraryId, itineraryId);
    }

    @Test
    void addingAHotelLineItemPricesItThroughTheRealPersistedMarkupRuleBOK03() {
        UUID consultantId = UUID.randomUUID();
        authenticateAsSuperAdmin();
        UUID itineraryId = insertDraftItinerary(consultantId);
        paymentsApi.configureMarkup(consultantId, new ConfigureMarkupCommand(
            ProductCategory.HOTEL, MarkupType.PERCENTAGE, BigDecimal.valueOf(15), null, null));

        var command = new AddHotelLineItemCommand(SupplierId.HOTELBEDS, "rate-key-1", "Taj Palace", "Deluxe Room",
            MealPlan.BB, Instant.now().plusSeconds(3600), new Money(BigDecimal.valueOf(100), CurrencyCode.INR),
            CurrencyCode.INR, BigDecimal.valueOf(96), BigDecimal.valueOf(3), BigDecimal.ZERO);

        UUID lineItemId = bookingApi.addHotelLineItem(itineraryId, command);

        BigDecimal sellRate = jdbcTemplate.queryForObject(
            "SELECT sell_rate FROM hotel_line_item WHERE line_item_id = ?", BigDecimal.class, lineItemId);
        assertThat(sellRate).isEqualByComparingTo("11371.2000");
    }

    @Test
    void savingAnItineraryWithALineItemAsQuotationPublishesTheEventAndPersistsTheQuotationBOK08BOK09(Scenario scenario) {
        UUID consultantId = UUID.randomUUID();
        authenticateAsSuperAdmin();
        UUID itineraryId = insertDraftItinerary(consultantId);
        paymentsApi.configureMarkup(consultantId, new ConfigureMarkupCommand(
            ProductCategory.HOTEL, MarkupType.PERCENTAGE, BigDecimal.valueOf(15), null, null));
        bookingApi.addHotelLineItem(itineraryId, new AddHotelLineItemCommand(SupplierId.HOTELBEDS, "rate-key-1",
            "Taj Palace", "Deluxe Room", MealPlan.BB, Instant.now().plusSeconds(3600),
            new Money(BigDecimal.valueOf(100), CurrencyCode.INR), CurrencyCode.INR,
            BigDecimal.valueOf(96), BigDecimal.valueOf(3), BigDecimal.ZERO));

        scenario.stimulate(() -> bookingApi.saveAsQuotation(itineraryId))
            .andWaitForEventOfType(ItineraryQuotationSavedEvent.class)
            .matchingMappedValue(ItineraryQuotationSavedEvent::itineraryId, itineraryId);
    }

    @Test
    void savingAnItineraryWithNoLineItemsAsQuotationIsRejectedBOK08() {
        UUID consultantId = UUID.randomUUID();
        authenticateAsSuperAdmin();
        UUID itineraryId = insertDraftItinerary(consultantId);

        assertThatThrownBy(() -> bookingApi.saveAsQuotation(itineraryId))
            .isInstanceOf(IllegalStateException.class);

        String status = jdbcTemplate.queryForObject(
            "SELECT status FROM itinerary WHERE itinerary_id = ?", String.class, itineraryId);
        assertThat(status).isEqualTo("DRAFT");
    }

    @Test
    void savingAsQuotationPersistsAQuotationRowWithAFutureValidUntilBOK09() {
        UUID consultantId = UUID.randomUUID();
        authenticateAsSuperAdmin();
        UUID itineraryId = insertDraftItinerary(consultantId);
        paymentsApi.configureMarkup(consultantId, new ConfigureMarkupCommand(
            ProductCategory.HOTEL, MarkupType.PERCENTAGE, BigDecimal.valueOf(15), null, null));
        bookingApi.addHotelLineItem(itineraryId, new AddHotelLineItemCommand(SupplierId.HOTELBEDS, "rate-key-1",
            "Taj Palace", "Deluxe Room", MealPlan.BB, Instant.now().plusSeconds(3600),
            new Money(BigDecimal.valueOf(100), CurrencyCode.INR), CurrencyCode.INR,
            BigDecimal.valueOf(96), BigDecimal.valueOf(3), BigDecimal.ZERO));

        UUID quotationId = bookingApi.saveAsQuotation(itineraryId);

        Instant validUntil = jdbcTemplate.queryForObject(
            "SELECT valid_until FROM quotation WHERE quotation_id = ?", Instant.class, quotationId);
        assertThat(validUntil).isAfter(Instant.now());
        Boolean sharedWithTraveler = jdbcTemplate.queryForObject(
            "SELECT shared_with_traveler FROM quotation WHERE quotation_id = ?", Boolean.class, quotationId);
        assertThat(sharedWithTraveler).isFalse();
    }

    @Test
    void addingALineItemAfterSaveAsQuotationIsRejectedBOK08() {
        UUID consultantId = UUID.randomUUID();
        authenticateAsSuperAdmin();
        UUID itineraryId = insertDraftItinerary(consultantId);
        paymentsApi.configureMarkup(consultantId, new ConfigureMarkupCommand(
            ProductCategory.HOTEL, MarkupType.PERCENTAGE, BigDecimal.valueOf(15), null, null));
        bookingApi.addHotelLineItem(itineraryId, new AddHotelLineItemCommand(SupplierId.HOTELBEDS, "rate-key-1",
            "Taj Palace", "Deluxe Room", MealPlan.BB, Instant.now().plusSeconds(3600),
            new Money(BigDecimal.valueOf(100), CurrencyCode.INR), CurrencyCode.INR,
            BigDecimal.valueOf(96), BigDecimal.valueOf(3), BigDecimal.ZERO));
        bookingApi.saveAsQuotation(itineraryId);

        var secondLineItem = new AddHotelLineItemCommand(SupplierId.HOTELBEDS, "rate-key-2", "Another Hotel",
            "Standard Room", MealPlan.RO, Instant.now().plusSeconds(3600),
            new Money(BigDecimal.valueOf(50), CurrencyCode.INR), CurrencyCode.INR,
            BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThatThrownBy(() -> bookingApi.addHotelLineItem(itineraryId, secondLineItem))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void convertingAQuotationToAPackagePublishesPackageCreatedEventBOK10(Scenario scenario) {
        UUID consultantId = UUID.randomUUID();
        authenticateAsSuperAdmin();
        UUID quotationId = savedQuotationWithOneLineItem(consultantId);
        var command = new ConvertQuotationToPackageCommand("Goa Getaway", "A relaxing beach trip",
            LocalDate.now().plusDays(30), LocalDate.now().plusDays(90), BigDecimal.valueOf(500), 4);

        scenario.stimulate(() -> bookingApi.convertQuotationToPackage(quotationId, command))
            .andWaitForEventOfType(PackageCreatedEvent.class)
            .matchingMappedValue(PackageCreatedEvent::consultantId, consultantId);
    }

    @Test
    void convertingAQuotationToAPackageAutoFillsBasePriceFromThePersistedSellRateBOK10() {
        UUID consultantId = UUID.randomUUID();
        authenticateAsSuperAdmin();
        UUID quotationId = savedQuotationWithOneLineItem(consultantId);
        var command = new ConvertQuotationToPackageCommand("Goa Getaway", "A relaxing beach trip",
            LocalDate.now().plusDays(30), LocalDate.now().plusDays(90), BigDecimal.valueOf(500), 4);

        UUID packageId = bookingApi.convertQuotationToPackage(quotationId, command);

        BigDecimal basePrice = jdbcTemplate.queryForObject(
            "SELECT base_price FROM travel_package WHERE package_id = ?", BigDecimal.class, packageId);
        assertThat(basePrice).isEqualByComparingTo("11371.2000");
        String status = jdbcTemplate.queryForObject(
            "SELECT status FROM travel_package WHERE package_id = ?", String.class, packageId);
        assertThat(status).isEqualTo("DRAFT");
    }

    @Test
    void publishingAPackagePublishesPackagePublishedEventBOK12(Scenario scenario) {
        UUID consultantId = UUID.randomUUID();
        authenticateAsSuperAdmin();
        UUID quotationId = savedQuotationWithOneLineItem(consultantId);
        var createCommand = new ConvertQuotationToPackageCommand("Goa Getaway", "A relaxing beach trip",
            LocalDate.now().plusDays(30), LocalDate.now().plusDays(90), BigDecimal.valueOf(500), 4);
        UUID packageId = bookingApi.convertQuotationToPackage(quotationId, createCommand);

        scenario.stimulate(() -> bookingApi.publishPackage(packageId, true))
            .andWaitForEventOfType(PackagePublishedEvent.class)
            .matchingMappedValue(PackagePublishedEvent::promotedViaAds, true);
    }

    @Test
    void aPublishedPackageBecomesVisibleToTheConsultantsUsersBOK12() {
        UUID consultantId = UUID.randomUUID();
        authenticateAsSuperAdmin();
        UUID quotationId = savedQuotationWithOneLineItem(consultantId);
        var createCommand = new ConvertQuotationToPackageCommand("Goa Getaway", "A relaxing beach trip",
            LocalDate.now().plusDays(30), LocalDate.now().plusDays(90), BigDecimal.valueOf(500), 4);
        UUID packageId = bookingApi.convertQuotationToPackage(quotationId, createCommand);

        Page<PackageView> beforePublish = bookingApi.findPublishedPackagesByConsultant(consultantId, PageRequest.of(0, 20));
        assertThat(beforePublish.getContent()).isEmpty();

        bookingApi.publishPackage(packageId, false);

        Page<PackageView> afterPublish = bookingApi.findPublishedPackagesByConsultant(consultantId, PageRequest.of(0, 20));
        assertThat(afterPublish.getContent()).extracting(PackageView::packageId).containsExactly(packageId);
    }

    private UUID savedQuotationWithOneLineItem(UUID consultantId) {
        UUID itineraryId = insertDraftItinerary(consultantId);
        paymentsApi.configureMarkup(consultantId, new ConfigureMarkupCommand(
            ProductCategory.HOTEL, MarkupType.PERCENTAGE, BigDecimal.valueOf(15), null, null));
        bookingApi.addHotelLineItem(itineraryId, new AddHotelLineItemCommand(SupplierId.HOTELBEDS, "rate-key-1",
            "Taj Palace", "Deluxe Room", MealPlan.BB, Instant.now().plusSeconds(3600),
            new Money(BigDecimal.valueOf(100), CurrencyCode.INR), CurrencyCode.INR,
            BigDecimal.valueOf(96), BigDecimal.valueOf(3), BigDecimal.ZERO));
        return bookingApi.saveAsQuotation(itineraryId);
    }

    private UUID insertDraftItinerary(UUID consultantId) {
        UUID itineraryId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO itinerary (itinerary_id, consultant_id, status, ai_generated, created_at, updated_at) " +
                "VALUES (?, ?, 'DRAFT', false, now(), now())",
            itineraryId, consultantId);
        return itineraryId;
    }

    private static void authenticateAs(Role role, UUID consultantId) {
        AdrenPrincipal principal = new AdrenPrincipal(UUID.randomUUID(), role, consultantId);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private static void authenticateAsSuperAdmin() {
        AdrenPrincipal principal = new AdrenPrincipal(UUID.randomUUID(), Role.SUPER_ADMIN, null);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
