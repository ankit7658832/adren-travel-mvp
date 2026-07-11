/**
 * AI Itinerary & Governance module (PRD Section 11). Wraps the Groq LLM
 * client behind a grounded-generation contract: the model may only select
 * from live SupplierSearchApi results, never fabricate a hotel/price. Every
 * suggestion is logged (PRD Section 11.2, 24.3 — 100% audit logging, no
 * sampling) before it reaches the Consultant for approval.
 * <p>
 * Scaffold status: package-info only — build out AiGovernanceApi,
 * GroqClient (internal), and AiSuggestionAuditLog following the pattern
 * established in the booking/ and supplier/ modules.
 */
@org.springframework.modulith.ApplicationModule(displayName = "AI Itinerary & Governance")
package com.adren.travel.ai;
