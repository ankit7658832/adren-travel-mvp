package com.adren.travel.dashboard.internal;

import com.adren.travel.dashboard.ConsultantDashboardView;
import com.adren.travel.dashboard.DashboardApi;
import com.adren.travel.dashboard.SuperAdminDashboardView;
import com.adren.travel.security.CurrentPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * PRD §9.5 — the Consultant/Super Admin dashboards' own composite read
 * endpoints (HRD-09/HRD-11). No path variable on {@code /consultant},
 * same "own wallet unless SUPER_ADMIN overrides" shape as {@code
 * payments.internal.WalletController#get}: a CONSULTANT/USER always gets
 * their own dashboard, the optional {@code consultantId} query param
 * exists only so SUPER_ADMIN can inspect a specific tenant's.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
class DashboardController {

    private final DashboardApi dashboardApi;

    DashboardController(DashboardApi dashboardApi) {
        this.dashboardApi = dashboardApi;
    }

    @GetMapping("/consultant")
    ConsultantDashboardView consultant(@RequestParam(required = false) UUID consultantId) {
        UUID target = consultantId != null ? consultantId : CurrentPrincipal.get().consultantId();
        return dashboardApi.findConsultantDashboard(target);
    }

    @GetMapping("/super-admin")
    SuperAdminDashboardView superAdmin() {
        return dashboardApi.findSuperAdminDashboard();
    }
}
