import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class DistributedRateLimiter {

    static class TokenBucket {
        private final long maxTokens;
        private final double refillRatePerMillis;
        private double tokens;
        private long lastRefillTime;

        public TokenBucket(long maxTokens, long refillPeriodMillis) {
            this.maxTokens = maxTokens;
            this.refillRatePerMillis = (double) maxTokens / refillPeriodMillis;
            this.tokens = maxTokens;
            this.lastRefillTime = System.currentTimeMillis();
        }

        public synchronized RateLimitResponse tryConsume() {
            refill();

            if (tokens >= 1) {
                tokens -= 1;
                return new RateLimitResponse(true, (long) tokens, 0);
            } else {
                long retryAfter = (long) ((1 - tokens) / refillRatePerMillis);
                return new RateLimitResponse(false, 0, retryAfter / 1000);
            }
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTime;
            double refillTokens = elapsed * refillRatePerMillis;
            if (refillTokens > 0) {
                tokens = Math.min(maxTokens, tokens + refillTokens);
                lastRefillTime = now;
            }
        }

        public synchronized Map<String, Long> getStatus() {
            refill();
            long used = maxTokens - (long) tokens;
            long resetTime = System.currentTimeMillis() + 
                    (long)((maxTokens - tokens) / refillRatePerMillis);
            Map<String, Long> status = new HashMap<>();
            status.put("used", used);
            status.put("limit", maxTokens);
            status.put("reset", resetTime / 1000);
            return status;
        }
    }

    static class RateLimitResponse {
        boolean allowed;
        long remaining;
        long retryAfterSeconds;

        RateLimitResponse(boolean allowed, long remaining, long retryAfterSeconds) {
            this.allowed = allowed;
            this.remaining = remaining;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public String toString() {
            if (allowed) {
                return "Allowed (" + remaining + " requests remaining)";
            } else {
                return "Denied (0 requests remaining, retry after " 
                        + retryAfterSeconds + "s)";
            }
        }
    }

    private ConcurrentHashMap<String, TokenBucket> clientBuckets = new ConcurrentHashMap<>();
    private final long maxRequests = 1000;
    private final long oneHourMillis = 60 * 60 * 1000;

    public RateLimitResponse checkRateLimit(String clientId) {
        TokenBucket bucket = clientBuckets.computeIfAbsent(
                clientId, 
                id -> new TokenBucket(maxRequests, oneHourMillis)
        );
        return bucket.tryConsume();
    }

    public Map<String, Long> getRateLimitStatus(String clientId) {
        TokenBucket bucket = clientBuckets.get(clientId);
        if (bucket == null) {
            Map<String, Long> empty = new HashMap<>();
            empty.put("used", 0L);
            empty.put("limit", maxRequests);
            empty.put("reset", System.currentTimeMillis() / 1000);
            return empty;
        }
        return bucket.getStatus();
    }

    public static void main(String[] args) {

        DistributedRateLimiter limiter = new DistributedRateLimiter();

        for (int i = 0; i < 1005; i++) {
            RateLimitResponse response = limiter.checkRateLimit("abc123");
            if (i < 5 || i > 995) {
                System.out.println(response);
            }
        }

        System.out.println(limiter.getRateLimitStatus("abc123"));
    }
}