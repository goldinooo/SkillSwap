package com.trendenden.backend.session.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

/**
 * One skill-swap lesson between a teacher and a learner. Maps to the "sessions" table.
 *
 * The other tables (matches, users, skills) are referenced by plain ids for now.
 * They become real relationships once the Match, User and Skill entities exist.
 */
@Entity
@Table(name = "sessions")
public class Session {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long matchId;

	@Column(nullable = false)
	private Long teacherId;

	@Column(nullable = false)
	private Long learnerId;

	@Column(nullable = false)
	private Long skillId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SessionStatus status;

	private Instant scheduledAt;

	private Instant completedAt;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	/** For JPA only: it builds an empty object, then fills the fields from the database row. */
	protected Session() {
	}

	/** A brand-new session always starts as REQUESTED. */
	public Session(Long matchId, Long teacherId, Long learnerId, Long skillId, Instant scheduledAt) {
		this.matchId = Objects.requireNonNull(matchId, "matchId");
		this.teacherId = Objects.requireNonNull(teacherId, "teacherId");
		this.learnerId = Objects.requireNonNull(learnerId, "learnerId");
		this.skillId = Objects.requireNonNull(skillId, "skillId");
		if (teacherId.equals(learnerId)) {
			throw new IllegalArgumentException("Teacher and learner must be different users");
		}
		this.scheduledAt = scheduledAt;
		this.status = SessionStatus.REQUESTED;
		this.createdAt = Instant.now();
	}

	public void confirm() {
		status = status.moveTo(SessionStatus.CONFIRMED);
	}

	public void complete() {
		status = status.moveTo(SessionStatus.COMPLETED);
		completedAt = Instant.now();
	}

	public void cancel() {
		status = status.moveTo(SessionStatus.CANCELLED);
	}

	public Long getId() {
		return id;
	}

	public Long getMatchId() {
		return matchId;
	}

	public Long getTeacherId() {
		return teacherId;
	}

	public Long getLearnerId() {
		return learnerId;
	}

	public Long getSkillId() {
		return skillId;
	}

	public SessionStatus getStatus() {
		return status;
	}

	public Instant getScheduledAt() {
		return scheduledAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
