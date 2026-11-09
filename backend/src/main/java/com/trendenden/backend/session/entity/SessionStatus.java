/*
defines the possible states of a session 
controls which transitions between those states are valid
*/

package com.trendenden.backend.session.entity;

import java.util.EnumSet;
import java.util.Set;

public enum SessionStatus { // state machine 

	// private consts enum intants
	REQUESTED, //ordinal(): 0
	CONFIRMED, // ""   	 : 1 ...
	COMPLETED,
	CANCELLED;

	private Set<SessionStatus> allowedNext() {
		return switch (this) {
			case REQUESTED -> EnumSet.of(CONFIRMED, CANCELLED); //EnumSet (long i, bit mask wrapper) contains only CONF, and CANC enums.
			case CONFIRMED -> EnumSet.of(COMPLETED, CANCELLED);
			case COMPLETED, CANCELLED -> EnumSet.noneOf(SessionStatus.class);
		};
	}

	//Given my current state, is the requested next state allowed?
	public boolean canMoveTo(SessionStatus next) {
		return allowedNext().contains(next);
	}

	public SessionStatus moveTo(SessionStatus next) {
		if (!canMoveTo(next)) {
			throw new IllegalStateException("Cannot move session from " + this + " to " + next);
		}
		return next;
	}

}
