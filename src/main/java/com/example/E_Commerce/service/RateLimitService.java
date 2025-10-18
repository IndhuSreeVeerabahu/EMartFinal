package com.example.E_Commerce.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimitService {

    private final ConcurrentHashMap<String, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long WINDOW_SIZE_MS = 60 * 1000; // 1 minute

    public boolean isAllowed(String clientId, String action) {
        String key = clientId + ":" + action;
        long currentTime = System.currentTimeMillis();
        
        RateLimitInfo info = rateLimitMap.computeIfAbsent(key, k -> new RateLimitInfo());
        
        // Clean old entries
        if (currentTime - info.getWindowStart() > WINDOW_SIZE_MS) {
            info.reset(currentTime);
        }
        
        int maxRequests = getMaxRequestsForAction(action);
        return info.getRequestCount().incrementAndGet() <= maxRequests;
    }

    public void recordFailedLogin(String clientId) {
        String key = clientId + ":login_failed";
        long currentTime = System.currentTimeMillis();
        
        RateLimitInfo info = rateLimitMap.computeIfAbsent(key, k -> new RateLimitInfo());
        
        if (currentTime - info.getWindowStart() > WINDOW_SIZE_MS) {
            info.reset(currentTime);
        }
        
        info.getRequestCount().incrementAndGet();
    }

    public boolean isLoginBlocked(String clientId) {
        String key = clientId + ":login_failed";
        RateLimitInfo info = rateLimitMap.get(key);
        
        if (info == null) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - info.getWindowStart() > WINDOW_SIZE_MS) {
            rateLimitMap.remove(key);
            return false;
        }
        
        return info.getRequestCount().get() >= MAX_LOGIN_ATTEMPTS;
    }

    private int getMaxRequestsForAction(String action) {
        switch (action) {
            case "login":
                return MAX_LOGIN_ATTEMPTS;
            case "register":
                return 3; // Limit registration attempts
            case "search":
                return 120; // Allow more search requests
            default:
                return MAX_REQUESTS_PER_MINUTE;
        }
    }

    private static class RateLimitInfo {
        private long windowStart;
        private AtomicInteger requestCount;

        public RateLimitInfo() {
            this.windowStart = System.currentTimeMillis();
            this.requestCount = new AtomicInteger(0);
        }

        public void reset(long newWindowStart) {
            this.windowStart = newWindowStart;
            this.requestCount.set(0);
        }

        public long getWindowStart() {
            return windowStart;
        }

        public AtomicInteger getRequestCount() {
            return requestCount;
        }
    }
}
