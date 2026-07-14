package com.adren.travel;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** A single {@link Clock} bean so time-based logic (e.g. {@code whitelabel}'s branding cache TTL) is fake-clock-testable. */
@Configuration
class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
