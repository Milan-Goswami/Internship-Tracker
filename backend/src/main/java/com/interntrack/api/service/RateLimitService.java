package com.interntrack.api.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    public static class BucketHolder {
        private final Bucket bucket;
        private final long capacity;
        private volatile long lastAccessTime;

        public BucketHolder(Bucket bucket, long capacity) {
            this.bucket = bucket;
            this.capacity = capacity;
            this.lastAccessTime = System.currentTimeMillis();
        }

        public Bucket getBucket() {
            this.lastAccessTime = System.currentTimeMillis();
            return bucket;
        }

        public boolean isIdle() {
            return bucket.getAvailableTokens() >= capacity;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }
    }

    private final ConcurrentHashMap<String, BucketHolder> cache = new ConcurrentHashMap<>();

    @Value("${ratelimit.cache.max-size:10000}")
    private int maxCacheSize = 10000;

    @Value("${ratelimit.login.ip.capacity:10}")
    private long loginIpCapacity;
    @Value("${ratelimit.login.ip.refill-minutes:${ratelimit.login.ip.refillMinutes:10}}")
    private long loginIpRefillMinutes;

    @Value("${ratelimit.login.account.capacity:5}")
    private long loginAccountCapacity;
    @Value("${ratelimit.login.account.refill-minutes:${ratelimit.login.account.refillMinutes:15}}")
    private long loginAccountRefillMinutes;

    @Value("${ratelimit.register.ip.capacity:3}")
    private long registerIpCapacity;
    @Value("${ratelimit.register.ip.refill-minutes:${ratelimit.register.ip.refillMinutes:60}}")
    private long registerIpRefillMinutes;

    @Value("${ratelimit.refresh.ip.capacity:20}")
    private long refreshIpCapacity;
    @Value("${ratelimit.refresh.ip.refill-minutes:${ratelimit.refresh.ip.refillMinutes:1}}")
    private long refreshIpRefillMinutes;

    public Bucket resolveBucket(String key) {
        if (cache.size() >= maxCacheSize) {
            cleanup();
        }
        return cache.computeIfAbsent(key, this::newBucketHolder).getBucket();
    }

    public boolean tryConsumeIp(String key) {
        return resolveBucket(key).tryConsume(1);
    }

    public boolean tryConsumeAccount(String normalizedEmail) {
        return resolveBucket("login:account:" + normalizedEmail).tryConsume(1);
    }

    public void resetAccount(String normalizedEmail) {
        cache.remove("login:account:" + normalizedEmail);
    }

    public int cleanup() {
        int initialSize = cache.size();
        // Remove completely idle / full buckets
        cache.entrySet().removeIf(entry -> entry.getValue().isIdle());
        // If still near max capacity under heavy churn, remove entries older than 5 minutes
        if (cache.size() >= (maxCacheSize * 0.8)) {
            long cutoff = System.currentTimeMillis() - 300_000L;
            cache.entrySet().removeIf(entry -> entry.getValue().getLastAccessTime() < cutoff);
        }
        return initialSize - cache.size();
    }

    public int getCacheSize() {
        return cache.size();
    }

    public long getLoginRetryAfterSeconds() {
        return Math.max(loginAccountRefillMinutes, loginIpRefillMinutes) * 60;
    }

    public long getRegisterRetryAfterSeconds() {
        return registerIpRefillMinutes * 60;
    }

    public long getRefreshRetryAfterSeconds() {
        return refreshIpRefillMinutes * 60;
    }

    private BucketHolder newBucketHolder(String key) {
        if (key.startsWith("login:ip:")) {
            Bucket bucket = Bucket.builder()
                    .addLimit(Bandwidth.builder().capacity(loginIpCapacity).refillGreedy(loginIpCapacity, Duration.ofMinutes(loginIpRefillMinutes)).build())
                    .build();
            return new BucketHolder(bucket, loginIpCapacity);
        } else if (key.startsWith("login:account:")) {
            Bucket bucket = Bucket.builder()
                    .addLimit(Bandwidth.builder().capacity(loginAccountCapacity).refillGreedy(loginAccountCapacity, Duration.ofMinutes(loginAccountRefillMinutes)).build())
                    .build();
            return new BucketHolder(bucket, loginAccountCapacity);
        } else if (key.startsWith("register:ip:")) {
            Bucket bucket = Bucket.builder()
                    .addLimit(Bandwidth.builder().capacity(registerIpCapacity).refillGreedy(registerIpCapacity, Duration.ofMinutes(registerIpRefillMinutes)).build())
                    .build();
            return new BucketHolder(bucket, registerIpCapacity);
        } else if (key.startsWith("refresh:ip:")) {
            Bucket bucket = Bucket.builder()
                    .addLimit(Bandwidth.builder().capacity(refreshIpCapacity).refillGreedy(refreshIpCapacity, Duration.ofMinutes(refreshIpRefillMinutes)).build())
                    .build();
            return new BucketHolder(bucket, refreshIpCapacity);
        }
        Bucket bucket = Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(100).refillGreedy(100, Duration.ofMinutes(1)).build())
                .build();
        return new BucketHolder(bucket, 100);
    }

    public void reset() {
        cache.clear();
    }
}
