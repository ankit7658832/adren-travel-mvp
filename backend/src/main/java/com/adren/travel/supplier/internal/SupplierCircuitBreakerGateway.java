package com.adren.travel.supplier.internal;

import com.adren.travel.supplier.SupplierId;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * One named circuit breaker per {@link SupplierId} (PRD §24.2 — "each
 * supplier integration isolated behind a circuit breaker — one supplier's
 * downtime must not degrade search latency for the others"). Generic over
 * the client's result type so a future supplier client (e.g. Mystifly, BOK-04)
 * reuses this gateway without a new story (BOK-26's own acceptance criteria).
 * <p>
 * Deliberately programmatic (a plain {@link CircuitBreakerRegistry}), not
 * annotation-based (no {@code resilience4j-spring-boot3}/AOP dependency) —
 * this keeps the breaker directly unit-testable without a Spring context and
 * avoids any self-invocation-proxy pitfalls, per {@code backend-best-practices}'
 * DI guidance.
 */
@Component
class SupplierCircuitBreakerGateway {

    private static final Logger log = LoggerFactory.getLogger(SupplierCircuitBreakerGateway.class);

    private final CircuitBreakerRegistry registry;

    SupplierCircuitBreakerGateway() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50.0f)
            .slidingWindowSize(5)
            .minimumNumberOfCalls(3)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(2)
            .build();
        this.registry = CircuitBreakerRegistry.of(config);
    }

    /**
     * Runs {@code supplierCall} through {@code supplierId}'s own breaker.
     * On an open breaker or a failed call, returns {@code fallback} instead
     * of propagating — matches PRD §10.2's "exclude this supplier from the
     * current search cycle" pattern (e.g. §10.2.1's Hotelbeds timeout
     * handling); a struggling supplier degrades to an empty contribution,
     * never fails the whole aggregated search for the other suppliers.
     */
    <T> List<T> call(SupplierId supplierId, Supplier<List<T>> supplierCall, List<T> fallback) {
        CircuitBreaker breaker = registry.circuitBreaker(supplierId.name());
        try {
            return breaker.executeSupplier(supplierCall);
        } catch (Exception e) {
            log.warn("Supplier {} excluded from this search cycle (breaker state: {}): {}",
                supplierId, breaker.getState(), e.getMessage());
            return fallback;
        }
    }

    CircuitBreaker.State stateOf(SupplierId supplierId) {
        return registry.circuitBreaker(supplierId.name()).getState();
    }
}
