package com.terraguard.backend.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    private Bucket createNewBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(60)
                        .refillGreedy(30, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    public Bucket resolveBucket(String ipAddress) {
        return cache.computeIfAbsent(ipAddress, ip -> createNewBucket());
    }
}