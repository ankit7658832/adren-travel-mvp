package com.adren.travel.booking.internal;

import com.adren.travel.booking.AtolDisclosureRequiredException;
import com.adren.travel.booking.InventoryNoLongerAvailableException;
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

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return ProblemDetailFactory.create(HttpStatus.FORBIDDEN,
            "https://docs.adren.travel/errors/forbidden", "Forbidden",
            "The authenticated principal is not authorized to perform this action.", request.getRequestURI());
    }
}
