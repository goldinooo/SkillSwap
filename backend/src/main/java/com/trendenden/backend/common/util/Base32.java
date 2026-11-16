package com.trendenden.backend.common.util;

import java.util.Locale;

/**
 * RFC 4648 Base32, without padding.
 *
 * <p>Written here because the JDK has no Base32 (only Base64) and nothing on the
 * classpath provides one. Authenticator apps require Base32 for the {@code secret}
 * parameter of an {@code otpauth://} URI, so there is no way around it.
 *
 * <p>Base32 rather than Base64 is the TOTP convention for a practical reason: the
 * alphabet is case-insensitive and has no look-alike characters, so a user can retype a
 * secret by hand when they cannot scan the QR code.
 */
public final class Base32 {

	private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

	private Base32() {
	}

	public static String encode(byte[] data) {
		StringBuilder out = new StringBuilder((data.length * 8 + 4) / 5);
		int buffer = 0;
		int bitsLeft = 0;
		for (byte b : data) {
			buffer = (buffer << 8) | (b & 0xFF);
			bitsLeft += 8;
			// Emit one character for every whole 5 bits available.
			while (bitsLeft >= 5) {
				out.append(ALPHABET.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
				bitsLeft -= 5;
			}
		}
		// Left-pad the final partial group with zero bits.
		if (bitsLeft > 0) {
			out.append(ALPHABET.charAt((buffer << (5 - bitsLeft)) & 0x1F));
		}
		return out.toString();
	}

	public static byte[] decode(String encoded) {
		String normalised = encoded.trim().replace("=", "").toUpperCase(Locale.ROOT);
		int buffer = 0;
		int bitsLeft = 0;
		byte[] out = new byte[normalised.length() * 5 / 8];
		int index = 0;
		for (char c : normalised.toCharArray()) {
			int value = ALPHABET.indexOf(c);
			if (value < 0) {
				throw new IllegalArgumentException("Not a Base32 character: " + c);
			}
			buffer = (buffer << 5) | value;
			bitsLeft += 5;
			if (bitsLeft >= 8) {
				out[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
				bitsLeft -= 8;
			}
		}
		return out;
	}
}
