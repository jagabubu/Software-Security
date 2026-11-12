package com.northbridge.casevault.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final int MAX_TRACKED_NAMES = 5000;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    private static class Attempt {
        int failures;
        Instant lockedUntil;
    }

    public void recordFailure(String username) {
        String key = normalise(username);
        // Memory cap: random usernames must not be able to grow this map forever
        if (attempts.size() >= MAX_TRACKED_NAMES && !attempts.containsKey(key)) {
            return;
        }
        attempts.compute(key, (k, existing) -> {
            Attempt attempt = (existing == null) ? new Attempt() : existing;
            attempt.failures++;
            if (attempt.failures >= MAX_FAILURES) {
                attempt.lockedUntil = Instant.now().plus(LOCK_DURATION);
                attempt.failures = 0;
            }
            return attempt;
        });
    }

    public boolean isLocked(String username) {
        Attempt attempt = attempts.get(normalise(username));
        return attempt != null
                && attempt.lockedUntil != null
                && Instant.now().isBefore(attempt.lockedUntil);
    }

    public void reset(String username) {
        attempts.remove(normalise(username));
    }

    private String normalise(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}