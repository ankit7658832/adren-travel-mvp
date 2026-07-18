package com.adren.travel.booking.internal;

import com.adren.travel.ai.AiServiceUnavailableException;
import com.adren.travel.booking.AiApprovalRequiredException;
import com.adren.travel.booking.AiPricingStaleException;
import com.adren.travel.booking.AtolDisclosureRequiredException;
import com.adren.travel.booking.InventoryNoLongerAvailableException;
import com.adren.travel.payments.CreditLimitExceededException;
import com.adren.travel.shared.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * RULES.md §3.3 — one {@code @ControllerAdvice} per module, not a global
 * one, since a module's exception vocabulary shouldn't need a central
 * class to know about every other module's exception types.
 */
@RestControllerAdvice(basePackages = "com.adren.travel.booking")
class BookingControllerAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        return ProblemDetailFactory.createValidationProblem(ex, request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return ProblemDetailFactory.create(HttpStatus.BAD_REQUEST,
            "https://docs.adren.travel/errors/invalid-request", "Invalid request", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ProblemDetail handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        return ProblemDetailFactory.create(HttpStatus.CONFLICT,
            "https://docs.adren.travel/errors/invalid-state-transition", "Invalid state transition",
            ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InventoryNoLongerAvailableException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ProblemDetail handleInventoryNoLongerAvailable(InventoryNoLongerAvailableException ex, HttpServletRequest request) {
        return ProblemDetailFactory.create(HttpStatus.CONFLICT,
            "https://docs.adren.travel/errors/inventory-no-longer-available", "No longer available",
            ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AtolDisclosureRequiredException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ProblemDetail handleAtolDisclosureRequired(AtolDisclosureRequiredException ex, HttpServletRequest request) {
        return ProblemDetailFactory.create(HttpStatus.CONFLICT,
            "https://docs.adren.travel/errors/atol-disclosure-required", "ATOL disclosure required",
            ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AiApprovalRequiredException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ProblemDetail handleAiApprovalRequired(AiApprovalRequiredException ex, HttpServletRequest request) {
        return ProblemDetailFactory.create(HttpStatus.CONFLICT,
            "https://docs.adren.travel/errors/ai-approval-required", "AI approval required",
            ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AiPricingStaleException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ProblemDetail handleAiPricingStale(AiPricingStaleException ex, HttpServletRequest request) {
        return ProblemDetailFactory.create(HttpStatus.CONFLICT,
            "https://docs.adren.travel/errors/ai-pricing-stale", "AI-suggested pricing has changed",
            ex.getMessage(), request.getRequestURI());
    }

    /**
     * Stage 4 Step C adversarial finding — before this handler existed, a
     * real Groq failure (auth/timeout/rate-limit exhausted) thrown from
     * {@code AiApi.generateItinerary}/{@code revalidateAiPricingAtBooking}
     * through {@code ItineraryController} had no matching handler here (the
     * exception's {@code ai.internal} origin type can't even be referenced
     * from this package, RULES.md §4.1), so it fell through to Spring's
     * generic {@code /error} handling — same class of gap {@link
     * #handleCreditLimitExceeded}'s own Javadoc already documents for
     * {@code CreditLimitExceededException}. {@code SecurityConfig} now also
     * permits {@code /error} itself (that gap was masking EVERY unmapped
     * exception in the app as a misleading 401, not just this one) — this
     * handler is still needed on top of that fix so a Groq failure gets the
     * correct 503, not a generic 500.
     */
    @ExceptionHandler(AiServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    ProblemDetail handleAiServiceUnavailable(AiServiceUnavailableException ex, HttpServletRequest request) {
        return ProblemDetailFactory.create(HttpStatus.SERVICE_UNAVAILABLE,
            "https://docs.adren.travel/errors/ai-service-unavailable", "AI service unavailable",
            ex.getMessage(), request.getRequestURI());
    }

    /**
     * FIN-08's {@code payments}-package exception, thrown from {@code
     * paymentsApi.placeHold}/{@code payOnAccount} deep inside {@code
     * confirmBooking}/{@code confirmBookingOnAccount} — those are {@code
     * booking}-package controller methods, so {@code
     * PaymentsControllerAdvice}'s {@code basePackages = "com.adren.travel.payments"}
     * scoping does NOT catch it here (a {@code @RestControllerAdvice}'s
     * {@code basePackages} scopes by the HANDLING controller's package, not
     * the exception's origin). Without this handler the real HTTP
     * confirmation path 401s via Spring's generic {@code /error} fallback
     * instead of the intended 409 — a gap {@code CreditLimitBreachIT}
     * (which calls {@code paymentsApi.placeHold} directly, never through
     * this controller) never exercised. Same mapping {@code
     * PaymentsControllerAdvice.handleCreditLimitExceeded} uses.
     */
    @ExceptionHandler(CreditLimitExceededException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ProblemDetail handleCreditLimitExceeded(CreditLimitExceededException ex, HttpServletRequest request) {
        return ProblemDetailFactory.create(HttpStatus.CONFLICT,
            "https://docs.adren.travel/errors/credit-limit-exceeded", "Top up required",
            ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return ProblemDetailFactory.create(HttpStatus.FORBIDDEN,
            "https://docs.adren.travel/errors/forbidden", "Forbidden",
            "The authenticated principal is not authorized to perform this action.", request.getRequestURI());
    }
}
