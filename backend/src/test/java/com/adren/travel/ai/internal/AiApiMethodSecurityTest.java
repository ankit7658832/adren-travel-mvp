package com.adren.travel.ai.internal;

import com.adren.travel.ai.AiApi;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AI-11's second acceptance criterion — only SUPER_ADMIN can browse the AI
 * governance/audit log (PRD §6) — proven against the real {@code
 * @PreAuthorize}-proxied {@link AiApi} bean, mirroring {@code
 * SupplierSearchApiMethodSecurityTest}'s DB-free approach.
 */
class AiApiMethodSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        GroqClient groqClient() {
            return Mockito.mock(GroqClient.class);
        }

        @Bean
        com.adren.travel.supplier.SupplierSearchApi supplierSearchApi() {
            return Mockito.mock(com.adren.travel.supplier.SupplierSearchApi.class);
        }

        @Bean
        AiSuggestionAuditLogRepository auditLogRepository() {
            return Mockito.mock(AiSuggestionAuditLogRepository.class);
        }

        @Bean
        AiSuggestionAuditLogRecorder auditLogRecorder() {
            return Mockito.mock(AiSuggestionAuditLogRecorder.class);
        }

        @Bean
        AiSuggestionApprovalRepository approvalRepository() {
            return Mockito.mock(AiSuggestionApprovalRepository.class);
        }

        @Bean
        AdCreativeAuditLogRecorder adCreativeAuditLogRecorder() {
            return Mockito.mock(AdCreativeAuditLogRecorder.class);
        }

        @Bean
        GroqProperties groqProperties() {
            return new GroqProperties("https://api.groq.com/openai/v1", "llama-3.3-70b-versatile", 15, 2);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        ApplicationEventPublisher applicationEventPublisher() {
            return Mockito.mock(ApplicationEventPublisher.class);
        }

        @Bean
        AiApi aiApi(GroqClient groqClient, com.adren.travel.supplier.SupplierSearchApi supplierSearchApi,
                    AiSuggestionAuditLogRepository auditLogRepository, AiSuggestionAuditLogRecorder auditLogRecorder,
                    AiSuggestionApprovalRepository approvalRepository,
                    AdCreativeAuditLogRecorder adCreativeAuditLogRecorder, GroqProperties groqProperties,
                    ObjectMapper objectMapper, ApplicationEventPublisher events) {
            return new AiServiceImpl(groqClient, supplierSearchApi, auditLogRepository, auditLogRecorder,
                approvalRepository, adCreativeAuditLogRecorder, groqProperties, objectMapper, events);
        }
    }

    private static AnnotationConfigApplicationContext context;
    private static AiApi aiApi;

    @BeforeAll
    static void startContext() {
        context = new AnnotationConfigApplicationContext(TestConfig.class);
        aiApi = context.getBean(AiApi.class);
    }

    @AfterAll
    static void stopContext() {
        context.close();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsAnUnauthenticatedCallerBeforeReachingTheServiceMethod() {
        assertThatThrownBy(() -> aiApi.findAuditLog(null, PageRequest.of(0, 20)))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void aConsultantCannotBrowseTheAiGovernanceAuditLogFND03() {
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());

        assertThatThrownBy(() -> aiApi.findAuditLog(null, PageRequest.of(0, 20)))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void aUserCannotBrowseTheAiGovernanceAuditLog() {
        authenticateAs(Role.USER, UUID.randomUUID());

        assertThatThrownBy(() -> aiApi.findAuditLog(null, PageRequest.of(0, 20)))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void aSuperAdminCanBrowseTheAiGovernanceAuditLog() {
        AiSuggestionAuditLogRepository auditLogRepository = context.getBean(AiSuggestionAuditLogRepository.class);
        Mockito.when(auditLogRepository.findAllByOrderByCreatedAtDesc(Mockito.any()))
            .thenReturn(org.springframework.data.domain.Page.empty());
        authenticateAs(Role.SUPER_ADMIN, null);

        assertThat(aiApi.findAuditLog(null, PageRequest.of(0, 20)).getContent()).isEmpty();
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
