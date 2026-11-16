package com.trendenden.backend.publicapi.dto;

import java.time.Instant;

/** Aggregate counts only: nothing here identifies an individual user. */
public record PlatformStatsResponse(long totalUsers, Instant generatedAt) {
}
