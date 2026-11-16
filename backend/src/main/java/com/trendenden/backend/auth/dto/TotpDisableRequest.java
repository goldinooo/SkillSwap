package com.trendenden.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Turning 2FA off re-checks the password, so a stolen access token cannot do it. */
public record TotpDisableRequest(@NotBlank String password) {
}
