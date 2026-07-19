package com.adren.travel.notification.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HRD-03 / RULES.md §2.2 — the dedup guard every notification listener claims against before dispatching. */
@ExtendWith(MockitoExtension.class)
class ProcessedEventDeduplicationServiceTest {

    @Mock
    ProcessedEventRepository repository;

    ProcessedEventDeduplicationService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new ProcessedEventDeduplicationService(repository);
    }

    @Test
    void claimsAnEventNeverSeenBeforeAndPersistsIt() {
        when(repository.existsByEventIdAndListenerName("event-1", "SomeListener")).thenReturn(false);

        boolean claimed = service.tryClaim("event-1", "SomeListener");

        assertThat(claimed).isTrue();
        ArgumentCaptor<ProcessedEvent> captor = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo("event-1");
        assertThat(captor.getValue().getListenerName()).isEqualTo("SomeListener");
    }

    @Test
    void fastPathRejectsAnAlreadyProcessedPairWithoutAttemptingTheInsert() {
        when(repository.existsByEventIdAndListenerName("event-1", "SomeListener")).thenReturn(true);

        boolean claimed = service.tryClaim("event-1", "SomeListener");

        assertThat(claimed).isFalse();
        verify(repository, org.mockito.Mockito.never()).saveAndFlush(any());
    }

    @Test
    void losingTheRaceOnTheUniqueConstraintIsANoOpNotAnException() {
        // The fast-path exists() check missed it (a genuine concurrent
        // redelivery), so the insert itself is what catches it — the real
        // guarantee, not the fast path.
        when(repository.existsByEventIdAndListenerName("event-1", "SomeListener")).thenReturn(false);
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        boolean claimed = service.tryClaim("event-1", "SomeListener");

        assertThat(claimed).isFalse();
    }
}
