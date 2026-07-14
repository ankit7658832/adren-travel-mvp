package com.adren.travel.whitelabel.internal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

record AddUserRequest(@NotBlank @Email String email, @NotBlank String displayName) {
}
