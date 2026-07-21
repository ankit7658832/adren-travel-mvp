/**
 * Reporting/Dashboard module (PRD §9.5, §21.5/§21.6, HRD-09/HRD-11) — a
 * cross-cutting composite-read concern spanning {@code booking}/{@code
 * payments}/{@code ads}/{@code ai}, not naturally owned by any single one
 * of them (unlike PNR search, which stays inside {@code booking} since
 * it's genuinely a booking-domain concept). Owns no entities of its own;
 * every field it returns is sourced from another module's public Api.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Reporting & Dashboard")
package com.adren.travel.dashboard;
