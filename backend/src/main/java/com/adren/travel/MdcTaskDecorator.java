package com.adren.travel;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Carries the calling thread's MDC context (traceId, per RULES.md §6.1)
 * across the {@code @Async} executor boundary backing
 * {@code @ApplicationModuleListener} — MDC is thread-local by default and
 * does not cross that hop on its own, which is exactly the gap this class
 * closes. Registered as a plain {@code @Component}: Spring Boot's
 * auto-configured task executor (backing {@code @Async} when no other
 * {@code Executor} bean is declared) picks up any {@code TaskDecorator}
 * bean automatically.
 */
@Component
class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> callerContext = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previousContext = MDC.getCopyOfContextMap();
            try {
                if (callerContext != null) {
                    MDC.setContextMap(callerContext);
                }
                runnable.run();
            } finally {
                if (previousContext != null) {
                    MDC.setContextMap(previousContext);
                } else {
                    MDC.clear();
                }
            }
        };
    }
}
