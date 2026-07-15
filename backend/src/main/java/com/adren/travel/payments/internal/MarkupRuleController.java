package com.adren.travel.payments.internal;

import com.adren.travel.payments.ConfigureMarkupCommand;
import com.adren.travel.payments.MarkupRuleView;
import com.adren.travel.payments.PaymentsApi;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * PRD §12.1 — per-Consultant, per-category markup configuration (FIN-01).
 * Controller depends on {@link PaymentsApi} only (RULES.md §1.2).
 */
@RestController
@RequestMapping("/api/v1/consultants/{consultantId}/markup-rules")
class MarkupRuleController {

    private final PaymentsApi paymentsApi;

    MarkupRuleController(PaymentsApi paymentsApi) {
        this.paymentsApi = paymentsApi;
    }

    @PutMapping
    void configure(@PathVariable UUID consultantId, @Valid @RequestBody ConfigureMarkupRequest request) {
        paymentsApi.configureMarkup(consultantId, new ConfigureMarkupCommand(request.category(),
            request.markupType(), request.percentageValue(), request.flatFeeAmount(), request.flatFeeCurrency()));
    }

    @GetMapping
    List<MarkupRuleView> list(@PathVariable UUID consultantId) {
        return paymentsApi.findMarkupRules(consultantId);
    }
}
