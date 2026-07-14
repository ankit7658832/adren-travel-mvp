package com.adren.travel.whitelabel.internal;

import com.adren.travel.security.CapabilityGrantService.Capability;
import com.adren.travel.shared.PageResponse;
import com.adren.travel.whitelabel.AddUserCommand;
import com.adren.travel.whitelabel.ConsultantUserView;
import com.adren.travel.whitelabel.WhitelabelApi;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * PRD §3.3/§21.6 — a Consultant managing Users under their own account
 * (FND-09). Controller depends on {@link WhitelabelApi} only (RULES.md §1.2).
 */
@RestController
@RequestMapping("/api/v1/users")
class ConsultantUserController {

    private final WhitelabelApi whitelabelApi;

    ConsultantUserController(WhitelabelApi whitelabelApi) {
        this.whitelabelApi = whitelabelApi;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> addUser(@Valid @RequestBody AddUserRequest request) {
        UUID userId = whitelabelApi.addUser(new AddUserCommand(request.email(), request.displayName()));
        return Map.of("userId", userId);
    }

    @PatchMapping("/{userId}/capabilities/{capability}")
    void setCapability(@PathVariable UUID userId, @PathVariable Capability capability,
                       @RequestBody SetCapabilityRequest request) {
        whitelabelApi.setUserCapability(userId, capability, request.granted());
    }

    @GetMapping
    PageResponse<ConsultantUserView> listUsers(Pageable pageable) {
        return PageResponse.of(whitelabelApi.findUsersByConsultant(pageable));
    }
}
