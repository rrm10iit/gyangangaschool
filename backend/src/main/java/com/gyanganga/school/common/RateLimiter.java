package com.gyanganga.school.common;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Small in-memory sliding-window limiter, sufficient for a single-instance deployment. */
@Component
public class RateLimiter {
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    /** Records an attempt and returns false if the key already used up its quota in the window. */
    public boolean tryAcquire(String key, int max, Duration window) {
        long now = System.currentTimeMillis();
        long cutoff = now - window.toMillis();
        Deque<Long> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && q.peekFirst() < cutoff) q.pollFirst();
            if (q.size() >= max) return false;
            q.addLast(now);
            return true;
        }
    }

    /** Checks the quota without recording an attempt. */
    public boolean isBlocked(String key, int max, Duration window) {
        Deque<Long> q = hits.get(key);
        if (q == null) return false;
        long cutoff = System.currentTimeMillis() - window.toMillis();
        synchronized (q) {
            while (!q.isEmpty() && q.peekFirst() < cutoff) q.pollFirst();
            return q.size() >= max;
        }
    }

    public void record(String key) {
        Deque<Long> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            q.addLast(System.currentTimeMillis());
        }
    }

    public void reset(String key) {
        hits.remove(key);
    }

    @Scheduled(fixedDelay = 600_000)
    void cleanup() {
        long cutoff = System.currentTimeMillis() - Duration.ofHours(2).toMillis();
        hits.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                return e.getValue().isEmpty() || e.getValue().peekLast() < cutoff;
            }
        });
    }
}
