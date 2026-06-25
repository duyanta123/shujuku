package com.labcourse.service;

import com.labcourse.entity.LoginAttempt;
import com.labcourse.repository.LoginAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class LoginAttemptService {

    private static final Logger logger = LoggerFactory.getLogger(LoginAttemptService.class);

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;
    private static final int ATTEMPT_WINDOW_MINUTES = 15;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Autowired
    private EntityManager entityManager;

    public LoginResult checkLoginAttempt(String key) {
        Optional<LoginAttempt> opt = loginAttemptRepository.findById(key);

        if (opt.isEmpty()) {
            return LoginResult.ALLOWED;
        }

        LoginAttempt attempt = opt.get();

        if (isLocked(attempt)) {
            long remainingMinutes = getRemainingLockMinutes(attempt);
            logger.warn("账号 {} 已被锁定，剩余 {} 分钟", key, remainingMinutes);
            return LoginResult.locked(remainingMinutes);
        }

        if (isWindowExpired(attempt)) {
            loginAttemptRepository.deleteById(key);
            return LoginResult.ALLOWED;
        }

        return LoginResult.ALLOWED;
    }

    @Transactional
    public void recordFailedAttempt(String key) {
        LoginAttempt attempt = entityManager.find(LoginAttempt.class, key, LockModeType.PESSIMISTIC_WRITE);
        if (attempt == null) {
            try {
                attempt = new LoginAttempt();
                attempt.setAttemptKey(key);
                attempt.setAttempts(0);
                attempt.setFirstAttemptTime(LocalDateTime.now());
                entityManager.persist(attempt);
                entityManager.flush();
            } catch (Exception e) {
                attempt = entityManager.find(LoginAttempt.class, key, LockModeType.PESSIMISTIC_WRITE);
                if (attempt == null) {
                    throw e;
                }
            }
        }

        if (isWindowExpired(attempt)) {
            attempt.setAttempts(0);
            attempt.setFirstAttemptTime(LocalDateTime.now());
            attempt.setLockUntil(null);
        }

        attempt.setAttempts(attempt.getAttempts() + 1);

        if (attempt.getAttempts() >= MAX_ATTEMPTS) {
            attempt.setLockUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            logger.warn("账号 {} 登录失败 {} 次，已锁定 {} 分钟", key, MAX_ATTEMPTS, LOCK_DURATION_MINUTES);
        }

        loginAttemptRepository.save(attempt);
    }

    public void resetAttempts(String key) {
        loginAttemptRepository.deleteById(key);
        logger.info("账号 {} 登录成功，已重置失败计数", key);
    }

    public int getRemainingAttempts(String key) {
        Optional<LoginAttempt> opt = loginAttemptRepository.findById(key);
        if (opt.isEmpty() || isWindowExpired(opt.get())) {
            return MAX_ATTEMPTS;
        }
        return Math.max(0, MAX_ATTEMPTS - opt.get().getAttempts());
    }

    private boolean isLocked(LoginAttempt attempt) {
        return attempt.getLockUntil() != null && LocalDateTime.now().isBefore(attempt.getLockUntil());
    }

    private boolean isWindowExpired(LoginAttempt attempt) {
        return attempt.getFirstAttemptTime() != null
                && LocalDateTime.now().isAfter(attempt.getFirstAttemptTime().plusMinutes(ATTEMPT_WINDOW_MINUTES));
    }

    private long getRemainingLockMinutes(LoginAttempt attempt) {
        if (attempt.getLockUntil() == null) return 0;
        long remaining = java.time.Duration.between(LocalDateTime.now(), attempt.getLockUntil()).toMinutes();
        return Math.max(0, remaining);
    }

    public static class LoginResult {
        private final boolean allowed;
        private final boolean locked;
        private final long remainingLockMinutes;
        private final int remainingAttempts;

        public static final LoginResult ALLOWED = new LoginResult(true, false, 0, 0);

        private LoginResult(boolean allowed, boolean locked, long remainingLockMinutes, int remainingAttempts) {
            this.allowed = allowed;
            this.locked = locked;
            this.remainingLockMinutes = remainingLockMinutes;
            this.remainingAttempts = remainingAttempts;
        }

        public static LoginResult locked(long remainingMinutes) {
            return new LoginResult(false, true, remainingMinutes, 0);
        }

        public static LoginResult failed(int remainingAttempts) {
            return new LoginResult(false, false, 0, remainingAttempts);
        }

        public boolean isAllowed() { return allowed; }
        public boolean isLocked() { return locked; }
        public long getRemainingLockMinutes() { return remainingLockMinutes; }
        public int getRemainingAttempts() { return remainingAttempts; }
    }
}