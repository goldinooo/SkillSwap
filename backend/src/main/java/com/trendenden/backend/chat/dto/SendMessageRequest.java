package com.trendenden.backend.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
    @NotNull Long sessionId,
    @NotBlank @Size(max = 2000) String content
){}