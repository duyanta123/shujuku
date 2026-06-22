package com.labcourse.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_attempts")
public class LoginAttempt {

    @Id
    @Column(name = "attempt_key", length = 100)
    private String attemptKey;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "first_attempt_time")
    private LocalDateTime firstAttemptTime;

    @Column(name = "lock_until")
    private LocalDateTime lockUntil;

    public LoginAttempt() {}

    public String getAttemptKey() { return attemptKey; }
    public void setAttemptKey(String attemptKey) { this.attemptKey = attemptKey; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public LocalDateTime getFirstAttemptTime() { return firstAttemptTime; }
    public void setFirstAttemptTime(LocalDateTime firstAttemptTime) { this.firstAttemptTime = firstAttemptTime; }

    public LocalDateTime getLockUntil() { return lockUntil; }
    public void setLockUntil(LocalDateTime lockUntil) { this.lockUntil = lockUntil; }
}