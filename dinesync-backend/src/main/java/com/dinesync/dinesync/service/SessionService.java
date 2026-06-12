package com.dinesync.dinesync.service;

import com.dinesync.dinesync.model.CustomerSession;
import com.dinesync.dinesync.model.SessionStatus;
import com.dinesync.dinesync.repository.CustomerSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SessionService {

    @Autowired
    private CustomerSessionRepository sessionRepository;

    @Autowired
    private JwtService jwtService;

    /**
     * Creates a new ACTIVE session for the given table,
     * persists it to MySQL, and returns a signed JWT.
     */
    public String createSession(Integer tableId) {
        String sessionUuid = UUID.randomUUID().toString();

        CustomerSession session = new CustomerSession();
        session.setSessionUuid(sessionUuid);
        session.setTableId(tableId);
        session.setCheckedInAt(LocalDateTime.now());
        session.setLastActivityAt(LocalDateTime.now());
        session.setStatus(SessionStatus.ACTIVE);

        sessionRepository.save(session);

        return jwtService.generateToken(sessionUuid, tableId);
    }

    /**
     * Two-factor validation: checks JWT signature + expiry AND that the
     * embedded sessionUuid is still ACTIVE in MySQL.
     * This means revoked/expired sessions cannot ride a still-valid token.
     */
    public boolean isValidSession(String token) {
        if (token == null || token.isBlank() || !jwtService.isValid(token)) return false;
        String sessionUuid = jwtService.extractSessionUuid(token);
        if (sessionUuid == null) return false;
        return sessionRepository.findBySessionUuidAndStatus(sessionUuid, SessionStatus.ACTIVE).isPresent();
    }

    /**
     * Extracts the raw sessionUuid from a JWT (used by OrderService when linking orders).
     */
    public String extractSessionUuid(String token) {
        return jwtService.extractSessionUuid(token);
    }

    /**
     * Single-query activity bump (W4 fix) — no extra SELECT before the UPDATE.
     * Prevents zombie session expiry for actively-ordering customers.
     */
    public void updateLastActivity(String sessionUuid) {
        sessionRepository.touchLastActivity(sessionUuid, LocalDateTime.now());
    }

    /**
     * C2 fix (Phase 4): Billing-specific validation.
     * Accepts ACTIVE and CHECKED_OUT sessions (customers can view their final receipt).
     * Rejects EXPIRED sessions and invalid/tampered JWTs.
     */
    public boolean isValidSessionForBilling(String token) {
        if (token == null || token.isBlank() || !jwtService.isValid(token)) return false;
        String sessionUuid = jwtService.extractSessionUuid(token);
        if (sessionUuid == null) return false;
        return sessionRepository.findBySessionUuid(sessionUuid)
                .map(s -> s.getStatus() != SessionStatus.EXPIRED)
                .orElse(false);
    }
}
