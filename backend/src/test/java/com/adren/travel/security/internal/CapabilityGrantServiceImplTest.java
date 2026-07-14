package com.adren.travel.security.internal;

import com.adren.travel.security.CapabilityGrantService.Capability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CapabilityGrantServiceImplTest {

    @Mock
    CapabilityGrantRepository repository;

    CapabilityGrantServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CapabilityGrantServiceImpl(repository);
    }

    @Test
    void isGrantedReturnsFalseWhenNoGrantRowExists() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserIdAndCapability(userId, Capability.CREATE_PACKAGE)).thenReturn(Optional.empty());

        assertThat(service.isGranted(userId, Capability.CREATE_PACKAGE)).isFalse();
    }

    @Test
    void setGrantedCreatesANewRowWhenNoneExistsYet() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserIdAndCapability(userId, Capability.CREATE_PACKAGE)).thenReturn(Optional.empty());

        service.setGranted(userId, Capability.CREATE_PACKAGE, true);

        var captor = org.mockito.ArgumentCaptor.forClass(CapabilityGrant.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isGranted()).isTrue();
    }

    @Test
    void setGrantedUpdatesAnExistingRowInPlace() {
        UUID userId = UUID.randomUUID();
        CapabilityGrant existing = new CapabilityGrant(UUID.randomUUID(), userId, Capability.CREATE_PACKAGE, false);
        when(repository.findByUserIdAndCapability(userId, Capability.CREATE_PACKAGE)).thenReturn(Optional.of(existing));

        service.setGranted(userId, Capability.CREATE_PACKAGE, true);

        assertThat(existing.isGranted()).isTrue();
        verify(repository).save(existing);
    }

    @Test
    void setGrantedCanRevokeAnExistingGrant() {
        UUID userId = UUID.randomUUID();
        CapabilityGrant existing = new CapabilityGrant(UUID.randomUUID(), userId, Capability.CREATE_PACKAGE, true);
        when(repository.findByUserIdAndCapability(userId, Capability.CREATE_PACKAGE)).thenReturn(Optional.of(existing));

        service.setGranted(userId, Capability.CREATE_PACKAGE, false);

        assertThat(existing.isGranted()).isFalse();
    }
}
