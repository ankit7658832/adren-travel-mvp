package com.adren.travel.ads.internal;

import com.adren.travel.ai.AiServiceUnavailableException;
import com.adren.travel.shared.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** RULES.md §3.3 — mirrors {@code booking.internal.BookingControllerAdvice}'s shape. */
@RestControllerAdvice(basePackages = "com.adren.travel.ads")
class AdsControllerAdvice {

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
            "https://docs.adren.travel/errors/invalid-state", "Invalid state", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return ProblemDetailFactory.create(HttpStatus.FORBIDDEN,
            "https://docs.adren.travel/errors/forbidden", "Forbidden",
            "The authenticated principal is not authorized to perform this action.", request.getRequestURI());
    }

    /** Stage 4 Step C adversarial finding — see {@code BookingControllerAdvice.handleAiServiceUnavailable}'s Javadoc; AI-12's ad-creative generation hits the same real Groq-failure path. */
    @ExceptionHandler(AiServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    ProblemDetail handleAiServiceUnavailable(AiServiceUnavailableException ex, HttpServletRequest request) {
        return ProblemDetailFactory.create(HttpStatus.SERVICE_UNAVAILABLE,
            "https://docs.adren.travel/errors/ai-service-unavailable", "AI service unavailable",
            ex.getMessage(), request.getRequestURI());
    }
}
