package com.adren.travel.notification.internal;

import com.adren.travel.notification.NotificationApi;
import com.adren.travel.notification.UpdateNotificationPreferenceCommand;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.whitelabel.WhitelabelApi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * HRD-04's role-scoping AC: only CONSULTANT can manage their own
 * notification preference — proven against the real {@code
 * @PreAuthorize}-proxied {@link NotificationApi} bean, mirroring {@code
 * AiApiMethodSecurityTest}'s DB-free approach.
 */
class NotificationApiMethodSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        NotificationPreferenceRepository notificationPreferenceRepository() {
            return Mockito.mock(NotificationPreferenceRepository.class);
        }

        @Bean
        WhitelabelApi whitelabelApi() {
            return Mockito.mock(WhitelabelApi.class);
        }

        @Bean
        SecondaryChannelProvider secondaryChannelProvider() {
            return new SecondaryChannelProvider();
        }

        @Bean
        ApplicationEventPublisher events() {
            return Mockito.mock(ApplicationEventPublisher.class);
        }

        @Bean
        NotificationApi notificationApi(NotificationPreferenceRepository repository, WhitelabelApi whitelabelApi,
                                         SecondaryChannelProvider secondaryChannelProvider, ApplicationEventPublisher events) {
            return new NotificationServiceImpl(repository, whitelabelApi, secondaryChannelProvider, events);
        }
    }

    private static AnnotationConfigApplicationContext context;
    private static NotificationApi notificationApi;

    @BeforeAll
    static void startContext() {
        context = new AnnotationConfigApplicationContext(TestConfig.class);
        notificationApi = context.getBean(NotificationApi.class);
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
    void aSuperAdminCannotUpdateANotificationPreference() {
        authenticateAs(Role.SUPER_ADMIN);

        assertThatThrownBy(() -> notificationApi.updateNotificationPreference(new UpdateNotificationPreferenceCommand("SMS")))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void aUserCannotUpdateANotificationPreference() {
        authenticateAs(Role.USER);

        assertThatThrownBy(() -> notificationApi.updateNotificationPreference(new UpdateNotificationPreferenceCommand("SMS")))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void aSuperAdminCannotFindANotificationPreference() {
        authenticateAs(Role.SUPER_ADMIN);

        assertThatThrownBy(notificationApi::findNotificationPreference).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void aUserCannotFindANotificationPreference() {
        authenticateAs(Role.USER);

        assertThatThrownBy(notificationApi::findNotificationPreference).isInstanceOf(AccessDeniedException.class);
    }

    private static void authenticateAs(Role role) {
        AdrenPrincipal principal = new AdrenPrincipal(UUID.randomUUID(), role,
            role == Role.SUPER_ADMIN ? null : UUID.randomUUID());
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
