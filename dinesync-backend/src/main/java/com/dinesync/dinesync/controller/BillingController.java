package com.dinesync.dinesync.controller;

import com.dinesync.dinesync.model.BillResponse;
import com.dinesync.dinesync.service.BillingService;
import com.dinesync.dinesync.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Phase 4: Billing endpoints.
 *
 * C2 fix: Both endpoints require a valid JWT in the Authorization header.
 * The JWT's embedded sessionUuid must match the path variable to prevent
 * one customer from viewing or checking out another table's session.
 *
 * CORS handled globally by CorsConfig — no @CrossOrigin needed here.
 */
@Slf4j
@RestController
@RequestMapping("/api/sessions")
public class BillingController {

    @Autowired
    private BillingService billingService;

    @Autowired
    private SessionService sessionService;

    // ─── helper ──────────────────────────────────────────────

    private String extractToken(String authHeader) {
        return (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7)
                : null;
    }

    /**
     * Validates the Bearer token and checks session UUID ownership.
     * Returns null on success, or an error ResponseEntity if auth fails.
     */
    private ResponseEntity<?> guardBilling(String sessionUuid, String authHeader) {
        String token = extractToken(authHeader);
        if (!sessionService.isValidSessionForBilling(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Missing or invalid Authorization header");
        }
        String tokenUuid = sessionService.extractSessionUuid(token);
        if (!sessionUuid.equals(tokenUuid)) {
            log.warn("Session UUID mismatch: path={}, token={}", sessionUuid,
                    tokenUuid != null ? tokenUuid.substring(0, 8) + "…" : "null");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You do not have permission to access this session's bill");
        }
        return null; // auth OK
    }

    // ─── endpoints ───────────────────────────────────────────

    /**
     * GET /api/sessions/{sessionUuid}/bill
     * Returns an itemized bill preview. Read-only — does not change session state.
     * Works for ACTIVE and CHECKED_OUT sessions (customer can review receipt after paying).
     */
    @GetMapping("/{sessionUuid}/bill")
    public ResponseEntity<?> getBill(
            @PathVariable String sessionUuid,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        ResponseEntity<?> authError = guardBilling(sessionUuid, authHeader);
        if (authError != null) return authError;

        try {
            BillResponse bill = billingService.generateBill(sessionUuid);
            return ResponseEntity.ok(bill);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/sessions/{sessionUuid}/checkout
     * Marks the session as CHECKED_OUT and returns the final bill.
     * Idempotent — safe to call again if the connection drops mid-request.
     */
    @PostMapping("/{sessionUuid}/checkout")
    public ResponseEntity<?> checkout(
            @PathVariable String sessionUuid,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        ResponseEntity<?> authError = guardBilling(sessionUuid, authHeader);
        if (authError != null) return authError;

        try {
            BillResponse bill = billingService.checkout(sessionUuid);
            return ResponseEntity.ok(bill);
        } catch (IllegalArgumentException e) {
            log.warn("Checkout failed for session {}: {}", sessionUuid, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
