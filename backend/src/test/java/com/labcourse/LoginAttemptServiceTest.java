package com.labcourse;

import com.labcourse.entity.LoginAttempt;
import com.labcourse.repository.LoginAttemptRepository;
import com.labcourse.service.LoginAttemptService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LoginAttemptServiceTest {

    @Autowired
    private LoginAttemptService service;

    @Autowired
    private LoginAttemptRepository repository;

    private String prefix;

    @BeforeEach
    void setUp() {
        prefix = "login-attempt-test-" + UUID.randomUUID() + "-";
    }

    @AfterEach
    void tearDown() {
        repository.findAll().stream()
                .map(LoginAttempt::getAttemptKey)
                .filter(key -> key != null && key.startsWith(prefix))
                .forEach(repository::deleteById);
    }

    @Test
    @DisplayName("checkLoginAttempt allows first attempt")
    void checkLoginAttempt_FirstAttempt_ShouldBeAllowed() {
        LoginAttemptService.LoginResult result = service.checkLoginAttempt(key("first"));
        assertTrue(result.isAllowed());
        assertFalse(result.isLocked());
    }

    @Test
    @DisplayName("recordFailedAttempt locks after max attempts")
    void recordFailedAttempt_FifthAttempt_ShouldLock() {
        String key = key("lock");
        for (int i = 0; i < 5; i++) {
            service.recordFailedAttempt(key);
        }

        LoginAttemptService.LoginResult result = service.checkLoginAttempt(key);
        assertFalse(result.isAllowed());
        assertTrue(result.isLocked());
        assertTrue(result.getRemainingLockMinutes() > 0);
    }

    @Test
    @DisplayName("checkLoginAttempt allows before lock threshold")
    void checkLoginAttempt_BeforeLock_ShouldBeAllowed() {
        String key = key("before-lock");
        for (int i = 0; i < 4; i++) {
            service.recordFailedAttempt(key);
        }

        LoginAttemptService.LoginResult result = service.checkLoginAttempt(key);
        assertTrue(result.isAllowed());
        assertFalse(result.isLocked());
        assertEquals(1, service.getRemainingAttempts(key));
    }

    @Test
    @DisplayName("resetAttempts clears persisted failures")
    void resetAttempts_AfterSuccess_ShouldReset() {
        String key = key("reset");
        service.recordFailedAttempt(key);
        service.recordFailedAttempt(key);
        assertTrue(repository.findById(key).isPresent());

        service.resetAttempts(key);

        assertTrue(repository.findById(key).isEmpty());
        assertTrue(service.checkLoginAttempt(key).isAllowed());
        assertEquals(5, service.getRemainingAttempts(key));
    }

    @Test
    @DisplayName("getRemainingAttempts decrements and never goes negative")
    void getRemainingAttempts_ShouldDecrementAndClamp() {
        String key = key("remaining");
        assertEquals(5, service.getRemainingAttempts(key));

        service.recordFailedAttempt(key);
        service.recordFailedAttempt(key);
        assertEquals(3, service.getRemainingAttempts(key));

        for (int i = 0; i < 10; i++) {
            service.recordFailedAttempt(key);
        }
        assertEquals(0, service.getRemainingAttempts(key));
    }

    @Test
    @DisplayName("expired attempt window is deleted and allowed")
    void windowExpired_ShouldResetCount() {
        String key = key("window-expired");
        repository.save(attempt(key, 2, LocalDateTime.now().minusMinutes(16), null));

        LoginAttemptService.LoginResult result = service.checkLoginAttempt(key);

        assertTrue(result.isAllowed());
        assertTrue(repository.findById(key).isEmpty());
        assertEquals(5, service.getRemainingAttempts(key));
    }

    @Test
    @DisplayName("expired lock allows login")
    void lockExpired_ShouldAllowLogin() {
        String key = key("lock-expired");
        repository.save(attempt(key, 5, LocalDateTime.now(), LocalDateTime.now().minusMinutes(1)));

        LoginAttemptService.LoginResult result = service.checkLoginAttempt(key);

        assertTrue(result.isAllowed());
        assertFalse(result.isLocked());
    }

    @Test
    @DisplayName("active lock reports bounded remaining minutes")
    void lockDuration_ShouldBeWithin30Minutes() {
        String key = key("active-lock");
        repository.save(attempt(key, 5, LocalDateTime.now(), LocalDateTime.now().plusMinutes(30)));

        LoginAttemptService.LoginResult result = service.checkLoginAttempt(key);

        assertFalse(result.isAllowed());
        assertTrue(result.isLocked());
        assertTrue(result.getRemainingLockMinutes() > 0);
        assertTrue(result.getRemainingLockMinutes() <= 30);
    }

    @Test
    @DisplayName("LoginResult factories expose expected state")
    void loginResult_Factories_ShouldHaveExpectedState() {
        LoginAttemptService.LoginResult locked = LoginAttemptService.LoginResult.locked(15);
        assertFalse(locked.isAllowed());
        assertTrue(locked.isLocked());
        assertEquals(15, locked.getRemainingLockMinutes());
        assertEquals(0, locked.getRemainingAttempts());

        assertTrue(LoginAttemptService.LoginResult.ALLOWED.isAllowed());
        assertFalse(LoginAttemptService.LoginResult.ALLOWED.isLocked());
        assertEquals(0, LoginAttemptService.LoginResult.ALLOWED.getRemainingLockMinutes());
        assertEquals(0, LoginAttemptService.LoginResult.ALLOWED.getRemainingAttempts());
    }

    private String key(String suffix) {
        return prefix + suffix;
    }

    private LoginAttempt attempt(String key, int attempts, LocalDateTime firstAttemptTime, LocalDateTime lockUntil) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setAttemptKey(key);
        attempt.setAttempts(attempts);
        attempt.setFirstAttemptTime(firstAttemptTime);
        attempt.setLockUntil(lockUntil);
        return attempt;
    }
}
