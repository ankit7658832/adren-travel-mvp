package com.adren.travel.notification.internal;

import com.adren.travel.ai.event.AiSuggestionGeneratedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * PRD §15, HRD-02 — "AI approval needed" trigger. {@link AiSuggestionGeneratedEvent}
 * fires for every AI generation attempt "however it resolved" (its own
 * Javadoc) — only a {@code SUGGESTED} disposition actually produced
 * something a Consultant needs to approve; {@code NO_VIABLE_SUGGESTION}
 * and {@code GROQ_ERROR} have nothing pending review (PRD §11.2 principle
 * 4 — "AI states inability rather than substituting", not a false
 * approval prompt). {@code disposition} is compared as a plain string
 * since {@code AiSuggestionDisposition} is package-private to {@code ai}
 * (RULES.md §4.1) — the event itself already made that choice.
 */
@Component
class AiApprovalNeededNotificationListener {

    private static final String SUGGESTED_DISPOSITION = "SUGGESTED";

    private final NotificationDispatcher dispatcher;

    AiApprovalNeededNotificationListener(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @ApplicationModuleListener
    void on(AiSuggestionGeneratedEvent event) {
        if (!SUGGESTED_DISPOSITION.equals(event.disposition())) {
            return;
        }
        String subject = "An AI itinerary suggestion needs your approval";
        String body = "An AI-generated suggestion is ready for your review on itinerary " + event.itineraryId() + ".";
        String message = "AI suggestion ready for approval on itinerary " + event.itineraryId() + ".";
        dispatcher.dispatch(event.consultantId(), subject, body, message);
    }
}
