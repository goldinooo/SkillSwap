package com.trendenden.backend.auth.dto;

/**
 * Returned once, when enrolment starts.
 *
 * @param secret      Base32, for users who cannot scan a QR code and type it instead
 * @param otpAuthUri  the {@code otpauth://} URI the frontend renders as a QR code
 */
public record TotpEnrollResponse(String secret, String otpAuthUri) {
}
