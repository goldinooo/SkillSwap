package com.trendenden.backend.session.entity;

import static com.trendenden.backend.session.entity.SessionStatus.CANCELLED;
import static com.trendenden.backend.session.entity.SessionStatus.COMPLETED;
import static com.trendenden.backend.session.entity.SessionStatus.CONFIRMED;
import static com.trendenden.backend.session.entity.SessionStatus.REQUESTED;

/**
 * Temporary test for SessionStatus. Delete once the feature is done.
 */
public class SessionStatusTest {

	private static int failures = 0;

	public static void main(String[] args) {
		// Legal moves -> should be true
		check(REQUESTED.canMoveTo(CONFIRMED), "REQUESTED -> CONFIRMED allowed");
		check(REQUESTED.canMoveTo(CANCELLED), "REQUESTED -> CANCELLED allowed");
		check(CONFIRMED.canMoveTo(COMPLETED), "CONFIRMED -> COMPLETED allowed");
		check(CONFIRMED.canMoveTo(CANCELLED), "CONFIRMED -> CANCELLED allowed");

		// Illegal moves -> should be false
		check(!REQUESTED.canMoveTo(COMPLETED), "REQUESTED -> COMPLETED blocked");
		check(!CONFIRMED.canMoveTo(REQUESTED), "CONFIRMED -> REQUESTED blocked");
		check(!COMPLETED.canMoveTo(CANCELLED), "COMPLETED -> CANCELLED blocked");
		check(!CANCELLED.canMoveTo(REQUESTED), "CANCELLED -> REQUESTED blocked");

		// moveTo returns the new status when legal
		check(REQUESTED.moveTo(CONFIRMED) == CONFIRMED, "moveTo returns CONFIRMED");

		// moveTo throws when illegal
		try {
			COMPLETED.moveTo(REQUESTED);
			check(false, "moveTo COMPLETED -> REQUESTED should throw");
		} catch (IllegalStateException e) {
			check(true, "moveTo COMPLETED -> REQUESTED throws");
		}

		System.out.println(failures == 0 ? "\nALL PASSED" : "\n" + failures + " FAILED");
		System.exit(failures == 0 ? 0 : 1);
	}

	private static void check(boolean ok, String label) {
		System.out.println((ok ? "PASS  " : "FAIL  ") + label);
		if (!ok) {
			failures++;
		}
	}

}
