package com.adren.travel.supplier.internal;

import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.ProductCategory;
import com.adren.travel.supplier.ActivateLocalDmcCommand;
import com.adren.travel.supplier.LocalDmcInventoryItemCommand;
import com.adren.travel.supplier.LocalDmcInventoryRowError;
import com.adren.travel.supplier.LocalDmcInventoryUploadResult;
import com.adren.travel.supplier.LocalDmcVerificationRequiredException;
import com.adren.travel.supplier.LocalDmcView;
import com.adren.travel.supplier.SubmitLocalDmcCommand;
import com.adren.travel.supplier.internal.localdmc.LocalDmcInventoryItem;
import com.adren.travel.supplier.internal.localdmc.LocalDmcRecord;
import com.adren.travel.supplier.internal.localdmc.LocalDmcRecordRepository;
import com.adren.travel.supplier.internal.localdmc.LocalDmcStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalDmcServiceTest {

    @Mock
    LocalDmcRecordRepository repository;

    @Mock
    com.adren.travel.supplier.internal.localdmc.LocalDmcInventoryItemRepository inventoryRepository;

    @Mock
    LocalDmcInventoryCsvParser csvParser;

    LocalDmcService service;

    @BeforeEach
    void setUp() {
        LocalDmcQualityThresholds thresholds = new LocalDmcQualityThresholds(new BigDecimal("0.2"), 3, 30);
        service = new LocalDmcService(repository, thresholds, inventoryRepository, csvParser);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitLocalDmcResolvesConsultantIdFromTheCallingPrincipalNotTheRequestFIN02() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);

        service.submitLocalDmc(new SubmitLocalDmcCommand("Goa Local Tours", List.of("TRANSFER", "ACTIVITY"),
            "City tour from 2000 INR", "Ref: partner@example.com"));

        ArgumentCaptor<LocalDmcRecord> captor = ArgumentCaptor.forClass(LocalDmcRecord.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getConsultantId()).isEqualTo(consultantId);
        assertThat(captor.getValue().getStatus()).isEqualTo(LocalDmcStatus.PENDING);
    }

    @Test
    void activateLocalDmcRejectsAConsultantActivatingAnotherConsultantsDmcFND03() {
        UUID ownerConsultantId = UUID.randomUUID();
        UUID localDmcId = UUID.randomUUID();
        LocalDmcRecord record = new LocalDmcRecord(localDmcId, ownerConsultantId, "Goa Local Tours",
            "TRANSFER", "City tour", "Ref");
        when(repository.findById(localDmcId)).thenReturn(Optional.of(record));
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());

        assertThatThrownBy(() -> service.activateLocalDmc(localDmcId, new ActivateLocalDmcCommand("Checked.")))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void activateLocalDmcWithoutVerificationNotesThrowsAndNeverSaves() {
        UUID consultantId = UUID.randomUUID();
        UUID localDmcId = UUID.randomUUID();
        LocalDmcRecord record = new LocalDmcRecord(localDmcId, consultantId, "Goa Local Tours",
            "TRANSFER", "City tour", "Ref");
        when(repository.findById(localDmcId)).thenReturn(Optional.of(record));
        authenticateAs(Role.CONSULTANT, consultantId);

        assertThatThrownBy(() -> service.activateLocalDmc(localDmcId, new ActivateLocalDmcCommand(null)))
            .isInstanceOf(LocalDmcVerificationRequiredException.class);
        verify(repository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void activateLocalDmcTransitionsToActiveAndPersists() {
        UUID consultantId = UUID.randomUUID();
        UUID localDmcId = UUID.randomUUID();
        LocalDmcRecord record = new LocalDmcRecord(localDmcId, consultantId, "Goa Local Tours",
            "TRANSFER", "City tour", "Ref");
        when(repository.findById(localDmcId)).thenReturn(Optional.of(record));
        authenticateAs(Role.CONSULTANT, consultantId);

        service.activateLocalDmc(localDmcId, new ActivateLocalDmcCommand("Checked license and references."));

        ArgumentCaptor<LocalDmcRecord> captor = ArgumentCaptor.forClass(LocalDmcRecord.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(LocalDmcStatus.ACTIVE);
    }

    @Test
    void findLocalDmcsAlwaysScopesAConsultantToTheirOwnRegardlessOfWhatWasRequestedFND03() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findByConsultantId(consultantId, pageable)).thenReturn(Page.empty());

        // Even passing a DIFFERENT consultantId (or null) never leaks
        // another tenant's data — the principal's own id always wins for
        // a non-Super-Admin caller.
        service.findLocalDmcs(UUID.randomUUID(), pageable);

        verify(repository).findByConsultantId(consultantId, pageable);
    }

    @Test
    void findLocalDmcsAsSuperAdminWithNoFilterReturnsEveryConsultantsRecords() {
        authenticateAs(Role.SUPER_ADMIN, null);
        Pageable pageable = PageRequest.of(0, 20);
        LocalDmcRecord record = new LocalDmcRecord(UUID.randomUUID(), UUID.randomUUID(), "Goa Local Tours",
            "TRANSFER", "City tour", "Ref");
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(record)));

        Page<LocalDmcView> result = service.findLocalDmcs(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(repository, org.mockito.Mockito.never()).findByConsultantId(any(), any());
    }

    @Test
    void bulkUploadPersistsEveryValidRowWhenTheParserReportsNoErrorsDMC03() {
        UUID consultantId = UUID.randomUUID();
        UUID localDmcId = UUID.randomUUID();
        LocalDmcRecord record = new LocalDmcRecord(localDmcId, consultantId, "Goa Local Tours", "TRANSFER", "x", "y");
        when(repository.findById(localDmcId)).thenReturn(Optional.of(record));
        authenticateAs(Role.CONSULTANT, consultantId);
        LocalDmcInventoryItemCommand row = new LocalDmcInventoryItemCommand("City Tour", ProductCategory.ACTIVITY,
            new BigDecimal("2000"), CurrencyCode.INR, "Free cancellation",
            java.time.LocalDate.of(2026, 8, 1), java.time.LocalDate.of(2026, 12, 31));
        when(csvParser.parse("csv-content"))
            .thenReturn(new LocalDmcInventoryCsvParser.ParseResult(List.of(row), List.of()));

        LocalDmcInventoryUploadResult result = service.bulkUploadLocalDmcInventory(localDmcId, "csv-content");

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.errors()).isEmpty();
        ArgumentCaptor<LocalDmcInventoryItem> captor = ArgumentCaptor.forClass(LocalDmcInventoryItem.class);
        verify(inventoryRepository).save(captor.capture());
        assertThat(captor.getValue().getProductName()).isEqualTo("City Tour");
        assertThat(captor.getValue().getLocalDmcId()).isEqualTo(localDmcId);
    }

    @Test
    void bulkUploadPersistsNothingWhenAnyRowHasErrorsDMC03() {
        UUID consultantId = UUID.randomUUID();
        UUID localDmcId = UUID.randomUUID();
        LocalDmcRecord record = new LocalDmcRecord(localDmcId, consultantId, "Goa Local Tours", "TRANSFER", "x", "y");
        when(repository.findById(localDmcId)).thenReturn(Optional.of(record));
        authenticateAs(Role.CONSULTANT, consultantId);
        LocalDmcInventoryItemCommand validRow = new LocalDmcInventoryItemCommand("City Tour", ProductCategory.ACTIVITY,
            new BigDecimal("2000"), CurrencyCode.INR, "Free cancellation",
            java.time.LocalDate.of(2026, 8, 1), java.time.LocalDate.of(2026, 12, 31));
        LocalDmcInventoryRowError rowError = new LocalDmcInventoryRowError(2, List.of("netRate is required"));
        when(csvParser.parse("csv-content"))
            .thenReturn(new LocalDmcInventoryCsvParser.ParseResult(List.of(validRow), List.of(rowError)));

        LocalDmcInventoryUploadResult result = service.bulkUploadLocalDmcInventory(localDmcId, "csv-content");

        assertThat(result.successCount()).isZero();
        assertThat(result.errors()).containsExactly(rowError);
        // All-or-nothing: NOT even the otherwise-valid row is persisted.
        verify(inventoryRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void bulkUploadRejectsAConsultantUploadingToAnotherConsultantsDmcFND03() {
        UUID ownerConsultantId = UUID.randomUUID();
        UUID localDmcId = UUID.randomUUID();
        LocalDmcRecord record = new LocalDmcRecord(localDmcId, ownerConsultantId, "Goa Local Tours", "TRANSFER", "x", "y");
        when(repository.findById(localDmcId)).thenReturn(Optional.of(record));
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());

        assertThatThrownBy(() -> service.bulkUploadLocalDmcInventory(localDmcId, "csv-content"))
            .isInstanceOf(AccessDeniedException.class);
        verify(inventoryRepository, org.mockito.Mockito.never()).save(any());
    }

    private static void authenticateAs(Role role, UUID consultantId) {
        AdrenPrincipal principal = new AdrenPrincipal(UUID.randomUUID(), role, consultantId);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
