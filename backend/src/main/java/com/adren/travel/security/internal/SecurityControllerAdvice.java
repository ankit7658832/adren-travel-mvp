package com.adren.travel.security.internal;

import com.adren.travel.shared.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** RULES.md §3.3 — mirrors {@code whitelabel.internal.WhitelabelControllerAdvice}'s shape. */
@RestControllerAdvice(basePackages = "com.adren.travel.security")
class SecurityControllerAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        return ProblemDetailFactory.createValidationProblem(ex, request.getRequestURI());
    }

    // AUTH-01 — a clear, login-specific 401 instead of the generic
    // "bearer token required" body RestAuthenticationEntryPoint would
    // otherwise produce if this exception fell through to the filter chain.
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    ProblemDetail handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return ProblemDetailFactory.create(HttpStatus.UNAUTHORIZED,
            "https://docs.adren.travel/errors/invalid-credentials", "Invalid credentials",
            "Invalid email or password.", request.getRequestURI());
    }

    // SCR-00b — an invalid/expired/already-used reset token.
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return ProblemDetailFactory.create(HttpStatus.BAD_REQUEST,
            "https://docs.adren.travel/errors/invalid-request", "Invalid request", ex.getMessage(), request.getRequestURI());
    }
}
