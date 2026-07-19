package com.adren.travel.supplier.internal;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HRD-12 — proves each sync job's cadence is config-driven (an
 * {@code ${property:default}} placeholder Spring resolves at startup, not a
 * hard-coded cron literal), and that the embedded default matches the
 * PRD-documented cadence (§10.2.1/§10.2.6/§10.2.7) when the property is
 * unset. Reflection over the annotation, not a booted context, is the
 * right tier here: whether Spring actually substitutes an overridden
 * property into a {@code @Scheduled} cron string is framework machinery,
 * not application logic — {@link SupplierContentSyncCadenceIT} proves that
 * substitution takes effect end to end against a real scheduler.
 */
class SupplierContentSyncCadenceTest {

    @Test
    void hotelSyncCadenceIsConfigDrivenWithTheNightlyPrdDefault() throws NoSuchMethodException {
        assertThat(cronOf("syncHotelContent")).isEqualTo("${adren.supplier.content-sync.hotels-cron:0 0 2 * * *}");
    }

    @Test
    void cruiseSyncCadenceIsConfigDrivenWithTheWeeklyPrdDefault() throws NoSuchMethodException {
        assertThat(cronOf("syncCruiseContent")).isEqualTo("${adren.supplier.content-sync.cruise-cron:0 0 3 * * MON}");
    }

    @Test
    void activitySyncCadenceIsConfigDrivenWithTheNightlyPrdDefault() throws NoSuchMethodException {
        assertThat(cronOf("syncActivityContent")).isEqualTo("${adren.supplier.content-sync.activities-cron:0 0 2 * * *}");
    }

    private static String cronOf(String methodName) throws NoSuchMethodException {
        Method method = SupplierContentSyncService.class.getDeclaredMethod(methodName);
        return method.getAnnotation(Scheduled.class).cron();
    }
}
