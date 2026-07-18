package com.adren.travel.supplier.internal;

import com.adren.travel.shared.PageResponse;
import com.adren.travel.supplier.ActivateLocalDmcCommand;
import com.adren.travel.supplier.LocalDmcInventoryItemCommand;
import com.adren.travel.supplier.LocalDmcInventoryItemView;
import com.adren.travel.supplier.LocalDmcInventoryUploadResult;
import com.adren.travel.supplier.LocalDmcView;
import com.adren.travel.supplier.SubmitLocalDmcCommand;
import com.adren.travel.supplier.SupplierSearchApi;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * HTTP surface for Local DMC onboarding/vetting (PRD §10.3, §20.14,
 * DMC-01/02) — thin, all logic lives behind {@link SupplierSearchApi}.
 */
@RestController
@RequestMapping("/api/v1/local-dmc")
class LocalDmcController {

    private final SupplierSearchApi supplierSearchApi;

    LocalDmcController(SupplierSearchApi supplierSearchApi) {
        this.supplierSearchApi = supplierSearchApi;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> submit(@Valid @RequestBody SubmitLocalDmcRequest request) {
        UUID localDmcId = supplierSearchApi.submitLocalDmc(new SubmitLocalDmcCommand(
            request.businessName(), request.productCategories(), request.sampleRatesSummary(), request.referencesInfo()));
        return Map.of("localDmcId", localDmcId);
    }

    @PostMapping("/{localDmcId}/activate")
    void activate(@PathVariable UUID localDmcId, @RequestBody ActivateLocalDmcRequest request) {
        supplierSearchApi.activateLocalDmc(localDmcId, new ActivateLocalDmcCommand(request.verificationNotes()));
    }

    @GetMapping
    PageResponse<LocalDmcView> findAll(@RequestParam(required = false) UUID consultantId, Pageable pageable) {
        return PageResponse.of(supplierSearchApi.findLocalDmcs(consultantId, pageable));
    }

    @PostMapping("/{localDmcId}/inventory/bulk-upload")
    LocalDmcInventoryUploadResult bulkUploadInventory(@PathVariable UUID localDmcId,
                                                        @Valid @RequestBody BulkUploadLocalDmcInventoryRequest request) {
        return supplierSearchApi.bulkUploadLocalDmcInventory(localDmcId, request.csvContent());
    }

    @GetMapping("/{localDmcId}/inventory")
    PageResponse<LocalDmcInventoryItemView> findInventory(@PathVariable UUID localDmcId, Pageable pageable) {
        return PageResponse.of(supplierSearchApi.findLocalDmcInventory(localDmcId, pageable));
    }

    @PatchMapping("/{localDmcId}/inventory/{itemId}")
    void updateInventoryItem(@PathVariable UUID localDmcId, @PathVariable UUID itemId,
                              @Valid @RequestBody UpdateLocalDmcInventoryItemRequest request) {
        supplierSearchApi.updateLocalDmcInventoryItem(localDmcId, itemId, new LocalDmcInventoryItemCommand(
            request.productName(), request.category(), request.netRate(), request.netRateCurrency(),
            request.cancellationPolicyText(), request.availableFrom(), request.availableTo()));
    }
}
