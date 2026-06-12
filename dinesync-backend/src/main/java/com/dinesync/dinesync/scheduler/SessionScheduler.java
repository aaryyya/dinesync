package com.dinesync.dinesync.scheduler;

import com.dinesync.dinesync.model.SessionStatus;
import com.dinesync.dinesync.repository.CustomerSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Phase 4: Zombie session cleaner.
 *
 * Runs every {@code session.expiry.check-interval-ms} milliseconds (default: 30 min).
 * Marks ACTIVE sessions as EXPIRED if their {@code lastActivityAt} is older than
 * {@code session.expiry.idle-timeout-hours} hours (default: 2 hours).
 *
 * Uses the bulk JPQL UPDATE in CustomerSessionRepository — one query, no N+1.
 */
@Slf4j
@Component
public class SessionScheduler {

    @Value("${session.expiry.idle-timeout-hours:2}")
    private int idleTimeoutHours;

    @Autowired
    private CustomerSessionRepository sessionRepository;

    @Scheduled(fixedRateString = "${session.expiry.check-interval-ms:1800000}")
    public void expireIdleSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(idleTimeoutHours);

        int expired = sessionRepository.expireIdleSessions(
                SessionStatus.ACTIVE,
                SessionStatus.EXPIRED,
                cutoff
        );

        if (expired > 0) {
            log.info("[SCHEDULER] Expired {} idle session(s) (lastActivity older than {} hours)",
                    expired, idleTimeoutHours);
        } else {
            log.debug("[SCHEDULER] No idle sessions to expire.");
        }
    }
}
