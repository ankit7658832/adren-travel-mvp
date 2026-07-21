package com.adren.travel.dashboard;

import org.springframework.security.access.prepost.PreAuthorize;

import java.util.UUID;

/**
 * Public API of the Reporting/Dashboard module. Other modules must depend
 * on this interface, never on classes under {@code com.adren.travel.dashboard.internal}
 * — though in practice nothing else in this codebase needs to (this
 * module is a terminal read-model consumer of {@code booking}/{@code
 * payments}/{@code ads}/{@code ai}, not a collaborator anything else calls
 * into).
 */
public interface DashboardApi {

    /**
     * The Consultant Dashboard (PRD §9.5, §21.5, HRD-09): bookings this
     * month, GMV, wallet balance, top packages, pending quotations, active
     * campaigns — each sourced from its owning module's own public Api.
     * Tenant-scoped inside each of those downstream calls (RULES.md §5.2),
     * not re-checked here, since every one of them already enforces it.
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT','USER')")
    ConsultantDashboardView findConsultantDashboard(UUID consultantId);

    /**
     * The Super Admin Dashboard / Global Reporting (PRD §9.5, §21.6,
     * HRD-11): all-Consultant GMV, per-supplier performance, an AI
     * governance summary, and ad spend across Consultants — platform
     * scope, no tenant filter, same "no 'my own' equivalent" shape every
     * downstream method here already carries.
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    SuperAdminDashboardView findSuperAdminDashboard();
}
