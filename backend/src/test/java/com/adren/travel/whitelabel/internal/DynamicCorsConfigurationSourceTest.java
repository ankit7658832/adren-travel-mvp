package com.adren.travel.whitelabel.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** FND-08 — RULES.md §5.4: CORS resolved per-request from the whitelabel domain registry, never a wildcard. */
@ExtendWith(MockitoExtension.class)
class DynamicCorsConfigurationSourceTest {

    @Mock
    RegisteredDomainsCache registeredDomainsCache;

    private DynamicCorsConfigurationSource corsConfigurationSource;

    @BeforeEach
    void setUp() {
        corsConfigurationSource = new DynamicCorsConfigurationSource(registeredDomainsCache);
    }

    @Test
    void allowsARequestFromAMappedConsultantDomain() {
        when(registeredDomainsCache.isRegistered("consultant.example.com")).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://consultant.example.com");

        CorsConfiguration configuration = corsConfigurationSource.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).containsExactly("https://consultant.example.com");
        assertThat(configuration.getAllowCredentials()).isTrue();
    }

    @Test
    void rejectsARequestFromAnUnmappedOriginFND08() {
        when(registeredDomainsCache.isRegistered("evil.example.com")).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://evil.example.com");

        CorsConfiguration configuration = corsConfigurationSource.getCorsConfiguration(request);

        assertThat(configuration).isNull();
    }

    @Test
    void rejectsARequestWithNoOriginHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(corsConfigurationSource.getCorsConfiguration(request)).isNull();
    }

    @Test
    void neverReturnsAWildcardAllowedOrigin() {
        when(registeredDomainsCache.isRegistered("consultant.example.com")).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://consultant.example.com");

        CorsConfiguration configuration = corsConfigurationSource.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).doesNotContain("*");
        assertThat(configuration.getAllowedOriginPatterns()).isNull();
    }

    @Test
    void rejectsAMalformedOriginHeaderRatherThanThrowing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "http://[invalid");

        assertThat(corsConfigurationSource.getCorsConfiguration(request)).isNull();
    }
}
