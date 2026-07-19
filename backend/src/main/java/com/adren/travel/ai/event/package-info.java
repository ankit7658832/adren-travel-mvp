/**
 * Domain events other modules may {@code @ApplicationModuleListener} —
 * mirrors {@code booking.event}/{@code payments.event}'s shape. Missing
 * until HRD-02 needed {@code notification} to consume {@link
 * com.adren.travel.ai.event.AiSuggestionGeneratedEvent} — nothing outside
 * {@code ai} had depended on this package before.
 */
@org.springframework.modulith.NamedInterface("event")
package com.adren.travel.ai.event;
