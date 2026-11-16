package com.trendenden.backend.publicapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @param name a label the owner recognises, e.g. "CI pipeline" */
public record CreateApiKeyRequest(@NotBlank @Size(max = 60) String name) {
}
