package com.adren.travel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class MdcTaskDecoratorTest {

    private final MdcTaskDecorator decorator = new MdcTaskDecorator();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void propagatesTheCallingThreadsMdcContextIntoTheDecoratedRunnable() throws InterruptedException {
        MDC.put("traceId", "trace-123");
        AtomicReference<String> observedOnOtherThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Runnable decorated = decorator.decorate(() -> {
            observedOnOtherThread.set(MDC.get("traceId"));
            latch.countDown();
        });

        Thread otherThread = new Thread(decorated);
        otherThread.start();
        latch.await();
        otherThread.join();

        assertThat(observedOnOtherThread.get()).isEqualTo("trace-123");
    }

    @Test
    void clearsTheDecoratedThreadsMdcAfterRunningWhenItHadNoPriorContext() throws InterruptedException {
        MDC.put("traceId", "trace-123");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> hadContextAfterRun = new AtomicReference<>();

        Runnable decorated = decorator.decorate(() -> {
            // no-op body — the decorator's own finally block is what we're testing
        });

        Thread otherThread = new Thread(() -> {
            decorated.run();
            hadContextAfterRun.set(MDC.getCopyOfContextMap() != null && !MDC.getCopyOfContextMap().isEmpty());
            latch.countDown();
        });
        otherThread.start();
        latch.await();
        otherThread.join();

        assertThat(hadContextAfterRun.get()).isFalse();
    }
}
