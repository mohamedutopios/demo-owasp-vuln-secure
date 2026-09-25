package com.utopios.taskforge.security;

import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A07 - limitation des tentatives d'authentification.
 * Implementation memoire pour le lab ; en production : backend partage (Redis)
 * et prise en compte de l'adresse source.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 300_000L; // 5 minutes

    private final Map<String, Deque<Long>> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String username) {
        if (username == null) {
            return false;
        }
        Deque<Long> hits = attempts.get(username.toLowerCase());
        if (hits == null) {
            return false;
        }
        purge(hits);
        return hits.size() >= MAX_ATTEMPTS;
    }

    public void recordFailure(String username) {
        if (username == null) {
            return;
        }
        Deque<Long> hits = attempts.computeIfAbsent(username.toLowerCase(),
                k -> new ArrayDeque<>());
        synchronized (hits) {
            purge(hits);
            hits.addLast(System.currentTimeMillis());
        }
    }

    public void reset(String username) {
        if (username != null) {
            attempts.remove(username.toLowerCase());
        }
    }

    private void purge(Deque<Long> hits) {
        long cutoff = System.currentTimeMillis() - WINDOW_MS;
        while (!hits.isEmpty() && hits.peekFirst() < cutoff) {
            hits.pollFirst();
        }
    }
}
