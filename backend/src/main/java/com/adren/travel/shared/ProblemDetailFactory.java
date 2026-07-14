package com.adren.travel.shared;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * RULES.md §3.3 — the RFC 7807 Problem Details *shape*, shared across every
 * module's own {@code @ControllerAdvice}. Deliberately not the per-module
 * {@code type}/{@code title} catalogue (a module's exception vocabulary
 * shouldn't require this class to know about every module's exception
 * types) — this is the plumbing every module's advice calls into.
 */
public final class ProblemDetailFactory {

    private ProblemDetailFactory() {
    }

    public static ProblemDetail create(HttpStatus status, String type, String title, String detail, String instance) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(status);
        problemDetail.setType(URI.create(type));
        problemDetail.setTitle(title);
        problemDetail.setDetail(detail);
        problemDetail.setInstance(URI.create(instance));
        problemDetail.setProperty("traceId", MDC.get(TraceIds.MDC_KEY));
        return problemDetail;
    }

    /** For {@code @Valid} Bean Validation failures — {@code errors[]} lists the specific field(s)/message(s). */
    public static ProblemDetail createValidationProblem(MethodArgumentNotValidException ex, String instance) {
        ProblemDetail problemDetail = create(HttpStatus.BAD_REQUEST,
            "https://docs.adren.travel/errors/validation-failed", "Validation failed",
            "One or more fields failed validation.", instance);
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(ProblemDetailFactory::toFieldError)
            .toList();
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    private static Map<String, String> toFieldError(FieldError fieldError) {
        return Map.of(
            "field", fieldError.getField(),
            "message", fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "invalid");
    }
}
