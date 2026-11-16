package com.trendenden.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 401. The caller is not identified: no token, expired token, wrong password.
 *
 * <p>Use one single message for "no such email" and "wrong password". Telling the two
 * apart hands an attacker a free account-enumeration oracle.
 */
public class UnauthorizedException extends ApiException {

	public UnauthorizedException(String code, String message) {
		super(HttpStatus.UNAUTHORIZED, code, message);
	}
}
