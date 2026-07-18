package com.adren.travel.booking.internal;

import com.adren.travel.ai.AiSuggestedLineItem;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

record ApproveAiSuggestionRequest(@NotNull UUID auditLogId, @NotEmpty List<AiSuggestedLineItem> finalLineItems) {
}
