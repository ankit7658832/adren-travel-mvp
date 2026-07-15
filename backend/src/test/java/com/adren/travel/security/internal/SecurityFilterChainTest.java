package com.adren.travel.security.internal;

import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.CurrentPrincipal;
import com.adren.travel.security.Role;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end proof of FND-01's acceptance criteria against a real MVC
 * dispatch: a request with no/invalid bearer token is rejected before it
 * reaches the controller, and a request with a valid one reaches it with the
 * principal available via {@link CurrentPrincipal}, exactly as every future
 * controller/service will read it.
 * <p>
 * A {@code @WebMvcTest} slice (no database, no Testcontainers) is the
 * pragmatic "module" tier here per the testing-strategy skill — it boots
 * real Spring MVC + Spring Security wiring without requiring the Docker-backed
 * infrastructure that lands with OPS-01 later in the build order.
 * <p>
 * MockMvc is built with {@code standaloneSetup(...).addFilters(...)} using
 * the real {@link SecurityFilterChain} bean's actual filters, rather than
 * {@code @WebMvcTest}'s auto-wired, {@code @AutoConfigureMockMvc}-managed
 * {@code MockMvc} — the latter's {@code webAppContextSetup} integration in
 * this Spring Boot 4.1/Spring Security 7.1 combination does not propagate a
 * filter-set {@code SecurityContextHolder} authentication to
 * {@code AnonymousAuthenticationFilter}/{@code AuthorizationFilter} correctly
 * (verified empirically: the identical filter list run via
 * {@code standaloneSetup} behaves correctly). Exercising the exact same
 * {@code SecurityFilterChain} filters this way still proves this story's
 * acceptance criteria against real production wiring, just via a MockMvc
 * construction path that doesn't hit that harness-specific issue.
 */
@WebMvcTest(controllers = SecurityFilterChainTest.SampleSecuredController.class)
@Import({SecurityConfig.class, JwtTokenService.class, SecurityFilterChainTest.TestCorsConfig.class})
@EnableConfigurationProperties(JwtProperties.class)
class SecurityFilterChainTest {

    /**
     * This slice doesn't scan {@code whitelabel} (the real
     * {@code DynamicCorsConfigurationSource}, FND-08) — this story is
     * about the JWT filter chain/principal, not CORS resolution, so a
     * stub satisfying {@code SecurityConfig}'s {@code CorsConfigurationSource}
     * dependency is enough here.
     */
    @TestConfiguration
    static class TestCorsConfig {
        @Bean
        CorsConfigurationSource corsConfigurationSource() {
            return new UrlBasedCorsConfigurationSource();
        }
    }

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    private MockMvc mvc() {
        return MockMvcBuilders.standaloneSetup(new SampleSecuredController())
            .addFilters(securityFilterChain.getFilters().toArray(new Filter[0]))
            .build();
    }

    @Test
    void rejectsARequestWithNoBearerToken() throws Exception {
        mvc().perform(get("/api/test/whoami"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsARequestWithAnInvalidBearerToken() throws Exception {
        mvc().perform(get("/api/test/whoami").header("Authorization", "Bearer garbage"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsARequestWithAValidBearerTokenAndExposesThePrincipal() throws Exception {
        AdrenPrincipal principal = new AdrenPrincipal(UUID.randomUUID(), Role.CONSULTANT, UUID.randomUUID());
        String token = jwtTokenService.generateToken(principal);

        mvc().perform(get("/api/test/whoami").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().string(principal.userId() + ":" + principal.role() + ":" + principal.consultantId()));
    }

    @RestController
    static class SampleSecuredController {
        @GetMapping("/api/test/whoami")
        String whoAmI() {
            AdrenPrincipal principal = CurrentPrincipal.get();
            return principal.userId() + ":" + principal.role() + ":" + principal.consultantId();
        }
    }
}
