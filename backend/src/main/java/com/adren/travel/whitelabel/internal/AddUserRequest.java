package com.adren.travel.whitelabel.internal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record AddUserRequest(
    @NotBlank @Email String email,
    @NotBlank String displayName,
    @NotBlank @Size(min = 8) String password
) {
}
