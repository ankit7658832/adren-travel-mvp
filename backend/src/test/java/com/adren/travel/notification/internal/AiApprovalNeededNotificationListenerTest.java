package com.adren.travel.notification.internal;

import com.adren.travel.ai.event.AiSuggestionGeneratedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * HRD-02 — "AI approval needed" trigger dispatches only on a {@code
 * SUGGESTED} disposition; {@code NO_VIABLE_SUGGESTION}/{@code GROQ_ERROR}
 * have nothing to approve (PRD §11.2 principle 4). HRD-03 — a redelivery
 * of the same event (same auditLogId) is a no-op.
 */
@ExtendWith(MockitoExtension.class)
class AiApprovalNeededNotificationListenerTest {

    @Mock
    NotificationDispatcher dispatcher;

    @Mock
    ProcessedEventDeduplicationService deduplicationService;

    @Test
    void dispatchesWhenTheDispositionIsSuggested() {
        AiApprovalNeededNotificationListener listener = new AiApprovalNeededNotificationListener(dispatcher, deduplicationService);
        UUID consultantId = UUID.randomUUID();
        UUID auditLogId = UUID.randomUUID();
        when(deduplicationService.tryClaim(auditLogId.toString(), "AiApprovalNeededNotificationListener")).thenReturn(true);
        AiSuggestionGeneratedEvent event = new AiSuggestionGeneratedEvent(
            auditLogId, UUID.randomUUID(), consultantId, "SUGGESTED");

        listener.on(event);

        verify(dispatcher).dispatch(eq(consultantId), any(), any(), any());
    }

    @Test
    void doesNotDispatchWhenNoViableSuggestionWasFound() {
        AiApprovalNeededNotificationListener listener = new AiApprovalNeededNotificationListener(dispatcher, deduplicationService);
        AiSuggestionGeneratedEvent event = new AiSuggestionGeneratedEvent(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "NO_VIABLE_SUGGESTION");

        listener.on(event);

        verifyNoInteractions(dispatcher);
        verifyNoInteractions(deduplicationService);
    }

    @Test
    void doesNotDispatchWhenTheGroqCallFailed() {
        AiApprovalNeededNotificationListener listener = new AiApprovalNeededNotificationListener(dispatcher, deduplicationService);
        AiSuggestionGeneratedEvent event = new AiSuggestionGeneratedEvent(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "GROQ_ERROR");

        listener.on(event);

        verify(dispatcher, never()).dispatch(any(), any(), any(), any());
    }

    @Test
    void aRedeliveredEventIsANoOpHRD03() {
        AiApprovalNeededNotificationListener listener = new AiApprovalNeededNotificationListener(dispatcher, deduplicationService);
        UUID auditLogId = UUID.randomUUID();
        when(deduplicationService.tryClaim(auditLogId.toString(), "AiApprovalNeededNotificationListener")).thenReturn(false);
        AiSuggestionGeneratedEvent event = new AiSuggestionGeneratedEvent(
            auditLogId, UUID.randomUUID(), UUID.randomUUID(), "SUGGESTED");

        listener.on(event);

        verifyNoInteractions(dispatcher);
    }
}
