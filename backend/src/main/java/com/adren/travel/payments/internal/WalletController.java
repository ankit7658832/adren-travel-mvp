package com.adren.travel.payments.internal;

import com.adren.travel.payments.PaymentsApi;
import com.adren.travel.payments.WalletView;
import com.adren.travel.security.CurrentPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * PRD §12.3/§21.7 — a Consultant's wallet balance/credit-limit/pending-holds
 * summary (FIN-06). No path variable per the story's own sub-task
 * ({@code GET /api/v1/wallet}): a CONSULTANT/USER always gets their own
 * wallet; the optional {@code consultantId} query param exists only so
 * SUPER_ADMIN can inspect a specific tenant's wallet (enforced by
 * {@code CurrentPrincipal.resolveTenantScope} inside the service, same as
 * every other tenant-scoped lookup).
 */
@RestController
@RequestMapping("/api/v1/wallet")
class WalletController {

    private final PaymentsApi paymentsApi;

    WalletController(PaymentsApi paymentsApi) {
        this.paymentsApi = paymentsApi;
    }

    @GetMapping
    WalletView get(@RequestParam(required = false) UUID consultantId) {
        UUID target = consultantId != null ? consultantId : CurrentPrincipal.get().consultantId();
        return paymentsApi.getWallet(target);
    }
}
