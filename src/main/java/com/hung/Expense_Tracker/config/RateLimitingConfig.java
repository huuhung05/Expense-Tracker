package com.hung.Expense_Tracker.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Rate-limiting configuration using Bucket4j 8.x.
 *
 * <p>Protects sensitive endpoints (e.g. /api/auth/login) from brute-force attacks.
 * The "loginLimiter" bean allows at most 5 requests per minute per server instance.
 *
 * <p>Inject {@link Bucket} wherever you need to check the limit, e.g.:
 * <pre>
 *   if (!loginLimiter.tryConsume(1)) {
 *       return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
 *   }
 * </pre>
 */
@Configuration
public class RateLimitingConfig {

    /**
     * Returns a {@link Bucket} that allows 5 tokens per minute (greedy refill).
     * The bean name matches the conventional rate-limit identifier used in controllers.
     */
    @Bean(name = "loginLimiter")
    public Bucket loginLimiter() {
        // Bucket4j 8.x API: use Bandwidth.builder() instead of deprecated Bandwidth.classic()
        Bandwidth limit = Bandwidth.builder()
                .capacity(5)
                .refillGreedy(5, Duration.ofMinutes(1))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
