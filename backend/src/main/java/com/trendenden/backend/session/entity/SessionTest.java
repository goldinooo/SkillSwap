package com.trendenden.backend.session.entity;

import static com.trendenden.backend.session.entity.SessionStatus.CANCELLED;
import static com.trendenden.backend.session.entity.SessionStatus.COMPLETED;
import static com.trendenden.backend.session.entity.SessionStatus.CONFIRMED;
import static com.trendenden.backend.session.entity.SessionStatus.REQUESTED;

/**
 * Temporary test for Session. Delete once the feature is done.
 */
public class SessionTest {

	private static int failures = 0;

	public static void main(String[] args) {
		// A new session starts as REQUESTED, with a creation time and no completion time
		Session s = new Session(1L, 10L, 20L, 5L, null);
		check(s.getStatus() == REQUESTED, "new session is REQUESTED");
		check(s.getCreatedAt() != null, "createdAt is set");
		check(s.getCompletedAt() == null, "completedAt is empty");

		// Happy path: confirm, then complete
		s.confirm();
		check(s.getStatus() == CONFIRMED, "confirm -> CONFIRMED");
		s.complete();
		check(s.getStatus() == COMPLETED, "complete -> COMPLETED");
		check(s.getCompletedAt() != null, "completedAt is set after complete");

		// A completed session can't be cancelled, and the status stays COMPLETED
		try {
			s.cancel();
			check(false, "cancel after complete should throw");
		} catch (IllegalStateException e) {
			check(s.getStatus() == COMPLETED, "cancel after complete throws, status unchanged");
		}

		// Cancel straight from REQUESTED
		Session s2 = new Session(1L, 10L, 20L, 5L, null);
		s2.cancel();
		check(s2.getStatus() == CANCELLED, "cancel from REQUESTED -> CANCELLED");

		// Can't complete without confirming first
		Session s3 = new Session(1L, 10L, 20L, 5L, null);
		try {
			s3.complete();
			check(false, "complete from REQUESTED should throw");
		} catch (IllegalStateException e) {
			check(s3.getCompletedAt() == null, "complete from REQUESTED throws, completedAt still empty");
		}

		// Teacher and learner can't be the same person
		try {
			new Session(1L, 10L, 10L, 5L, null);
			check(false, "teacher == learner should throw");
		} catch (IllegalArgumentException e) {
			check(true, "teacher == learner throws");
		}

		// Missing ids are rejected
		try {
			new Session(null, 10L, 20L, 5L, null);
			check(false, "null matchId should throw");
		} catch (NullPointerException e) {
			check(true, "null matchId throws");
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
