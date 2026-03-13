package com.terraguard.backend.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isBlank()) {
            ipAddress = request.getRemoteAddr();
        }

        try {
            Bucket bucket = rateLimiterService.resolveBucket(ipAddress);

            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

            if (probe.isConsumed()) {
                response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
                return true;
            } else {
                long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
                
                response.setStatus(429); 
                response.addHeader("Retry-After", String.valueOf(waitForRefill));
                response.getWriter().write("Too Many Requests. Try again in " + waitForRefill + " seconds.");
                
                return false; 
            }
        } catch (Exception e) {
            log.warn("[RATE LIMIT] Failed to check rate limit for {}, allowing request", ipAddress);
            return true; 
        }
    }
}