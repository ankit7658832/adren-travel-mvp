package com.adren.travel.supplier.internal;

import com.adren.travel.shared.ProblemDetailFactory;
import com.adren.travel.supplier.LocalDmcVerificationRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** RULES.md §3.3 — mirrors {@code booking.internal.BookingControllerAdvice}'s shape. */
@RestControllerAdvice(basePackages = "com.adren.travel.supplier")
class SupplierControllerAdvice {

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

    @ExceptionHandler(LocalDmcVerificationRequiredException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ProblemDetail handleLocalDmcVerificationRequired(LocalDmcVerificationRequiredException ex, HttpServletRequest request) {
        return ProblemDetailFactory.create(HttpStatus.CONFLICT,
            "https://docs.adren.travel/errors/local-dmc-verification-required", "Verification required",
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
