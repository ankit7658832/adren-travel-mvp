package com.adren.travel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * Explicit {@link AsyncConfigurer} — the executor backing every
 * {@code @Async} method (including {@code @ApplicationModuleListener},
 * Spring Modulith's meta-annotation for
 * {@code @Async @TransactionalEventListener(phase = AFTER_COMMIT)}) — so
 * {@link MdcTaskDecorator} is guaranteed to be attached (RULES.md §6.1)
 * rather than relying on Spring Boot's auto-configured executor bean,
 * which isn't present in every bootstrap context (e.g. Spring Modulith's
 * {@code @ApplicationModuleTest} slices).
 */
@Configuration
@EnableAsync
class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    private final MdcTaskDecorator mdcTaskDecorator;

    AsyncConfig(MdcTaskDecorator mdcTaskDecorator) {
        this.mdcTaskDecorator = mdcTaskDecorator;
    }

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("adren-async-");
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setTaskDecorator(mdcTaskDecorator);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) -> log.error(
            "Uncaught exception in async method {}", method.getName(), ex);
    }
}
