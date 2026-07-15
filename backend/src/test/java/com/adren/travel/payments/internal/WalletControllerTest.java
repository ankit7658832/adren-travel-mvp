package com.adren.travel.payments.internal;

import com.adren.travel.payments.PaymentsApi;
import com.adren.travel.payments.WalletView;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.shared.CurrencyCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WalletControllerTest {

    private final PaymentsApi paymentsApi = mock(PaymentsApi.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new WalletController(paymentsApi))
            .setControllerAdvice(new PaymentsControllerAdvice())
            .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsTheWalletForAnExplicitlyRequestedConsultantId() throws Exception {
        UUID consultantId = UUID.randomUUID();
        when(paymentsApi.getWallet(consultantId)).thenReturn(new WalletView(consultantId,
            BigDecimal.valueOf(1000), BigDecimal.valueOf(5000), BigDecimal.ZERO, CurrencyCode.INR, Instant.now()));

        mockMvc.perform(get("/api/v1/wallet").param("consultantId", consultantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.consultantId").value(consultantId.toString()))
            .andExpect(jsonPath("$.currency").value("INR"));
    }

    @Test
    void defaultsToTheCallersOwnConsultantIdWhenNoneIsGiven() throws Exception {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        when(paymentsApi.getWallet(consultantId)).thenReturn(new WalletView(consultantId,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, CurrencyCode.INR, Instant.now()));

        mockMvc.perform(get("/api/v1/wallet"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.consultantId").value(consultantId.toString()));
    }

    private static void authenticateAs(Role role, UUID consultantId) {
        AdrenPrincipal principal = new AdrenPrincipal(UUID.randomUUID(), role, consultantId);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
