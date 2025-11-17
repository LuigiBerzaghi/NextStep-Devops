package com.softcode.nextstep.service;

import com.softcode.nextstep.exception.RateLimitException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final Map<String, UsageWindow> counters = new ConcurrentHashMap<>();

    public void validateWithinLimit(String key, int limit, Duration windowSize) {
        counters.compute(
                key,
                (k, current) -> {
                    Instant now = Instant.now();
                    if (current == null || now.isAfter(current.windowStart().plus(windowSize))) {
                        return new UsageWindow(now, 1);
                    }
                    if (current.count() >= limit) {
                        throw new RateLimitException("error.rate.limit_exceeded");
                    }
                    return current.incremented();
                });
    }

    private record UsageWindow(Instant windowStart, int count) {
        UsageWindow incremented() {
            return new UsageWindow(windowStart, count + 1);
        }
    }
}
