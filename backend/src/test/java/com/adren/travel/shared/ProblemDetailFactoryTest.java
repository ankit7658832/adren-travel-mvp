package com.adren.travel.shared;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailFactoryTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void createBuildsTheRfc7807ShapeIncludingTheCurrentTraceId() {
        MDC.put(TraceIds.MDC_KEY, "trace-abc");

        ProblemDetail problemDetail = ProblemDetailFactory.create(
            HttpStatus.CONFLICT, "https://docs.adren.travel/errors/rate-expired", "Supplier rate expired",
            "The rate expired.", "/api/v1/itineraries/123/quotation");

        assertThat(problemDetail.getStatus()).isEqualTo(409);
        assertThat(problemDetail.getTitle()).isEqualTo("Supplier rate expired");
        assertThat(problemDetail.getDetail()).isEqualTo("The rate expired.");
        assertThat(problemDetail.getProperties()).containsEntry("traceId", "trace-abc");
    }

    @Test
    void createValidationProblemListsEachFieldError() throws NoSuchMethodException {
        Method method = Sample.class.getDeclaredMethod("target", String.class);
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "locationQueries", "must not be empty"));
        var ex = new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);

        ProblemDetail problemDetail = ProblemDetailFactory.createValidationProblem(ex, "/api/v1/search");

        assertThat(problemDetail.getStatus()).isEqualTo(400);
        @SuppressWarnings("unchecked")
        List<Map<String, String>> errors = (List<Map<String, String>>) problemDetail.getProperties().get("errors");
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).containsEntry("field", "locationQueries").containsEntry("message", "must not be empty");
    }

    static class Sample {
        void target(String arg) {
        }
    }
}
