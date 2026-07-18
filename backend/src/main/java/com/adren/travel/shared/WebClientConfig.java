package com.adren.travel.shared;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Registers a {@link WebClient.Builder} bean for real application startup.
 * <p>
 * Adversarial finding, Stage 4 Step C: Spring Boot 4 split {@code
 * WebClientAutoConfiguration} out of {@code spring-boot-starter-webflux}
 * (that starter is now server-side-reactive-only — {@code HttpHandler}/
 * {@code WebFlux} routing/sessions) into a separate module this project
 * never added, so the real application failed to start at all —
 * {@code GroqClient} (ai) and {@code HotelbedsClient}/{@code StubaClient}/
 * {@code TboClient} (supplier) all constructor-inject {@code
 * WebClient.Builder}, and with no bean to satisfy it, {@code ./gradlew
 * bootRun} threw {@code UnsatisfiedDependencyException} before any request
 * could ever reach a controller. This was invisible at every existing test
 * tier because every {@code @ApplicationModuleTest} class supplies its own
 * {@code @TestConfiguration}-scoped {@code WebClient.Builder} bean as a
 * workaround for the module-test slice's narrower auto-configuration —
 * masking, rather than exercising, this gap. This bean is the production
 * equivalent of those test workarounds, in {@code shared} (the one OPEN
 * module, since every module needing outbound HTTP shares this).
 */
@Configuration
class WebClientConfig {

    @Bean
    WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
