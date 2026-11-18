package com.gyanganga.school.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RateLimiterTest {
    private final RateLimiter limiter = new RateLimiter();
    private static final Duration HOUR = Duration.ofHours(1);

    @Test
    void allowsUpToTheLimitThenBlocks() {
        assertThat(limiter.tryAcquire("a", 2, HOUR)).isTrue();
        assertThat(limiter.tryAcquire("a", 2, HOUR)).isTrue();
        assertThat(limiter.tryAcquire("a", 2, HOUR)).isFalse();
    }

    @Test
    void keysAreIndependent() {
        limiter.tryAcquire("a", 1, HOUR);
        assertThat(limiter.tryAcquire("a", 1, HOUR)).isFalse();
        assertThat(limiter.tryAcquire("b", 1, HOUR)).isTrue();
    }

    @Test
    void attemptsOutsideTheWindowAreForgotten() throws InterruptedException {
        Duration tiny = Duration.ofMillis(50);
        limiter.tryAcquire("a", 1, tiny);
        assertThat(limiter.tryAcquire("a", 1, tiny)).isFalse();
        Thread.sleep(80);
        assertThat(limiter.tryAcquire("a", 1, tiny)).isTrue();
    }

    @Test
    void isBlockedDoesNotConsumeQuota() {
        assertThat(limiter.isBlocked("a", 1, HOUR)).isFalse();
        assertThat(limiter.isBlocked("a", 1, HOUR)).isFalse();
        limiter.record("a");
        assertThat(limiter.isBlocked("a", 1, HOUR)).isTrue();
    }

    @Test
    void resetClearsTheKey() {
        limiter.record("a");
        limiter.record("a");
        assertThat(limiter.isBlocked("a", 2, HOUR)).isTrue();
        limiter.reset("a");
        assertThat(limiter.isBlocked("a", 2, HOUR)).isFalse();
    }
}
