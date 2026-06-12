package com.dinesync.dinesync.service;

import com.dinesync.dinesync.model.*;
import com.dinesync.dinesync.repository.CustomerSessionRepository;
import com.dinesync.dinesync.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BillingService {

    @Autowired
    private CustomerSessionRepository sessionRepository;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * C3 fix: private helper that does the pure bill computation.
     *
     * Called directly (not via proxy) from both generateBill() and checkout()
     * to avoid the Spring AOP proxy bypass bug where a @Transactional(readOnly=true)
     * method called from a write-transaction method inherits the write transaction
     * instead of its own read-only transaction.
     *
     * This method has no @Transactional annotation — it runs inside whatever
     * transaction the calling public method opened.
     */
    private BillResponse buildBillInternal(CustomerSession session) {
        List<Order> orders = orderRepository
                .findBySessionUuidOrderByCreatedAtAsc(session.getSessionUuid());

        List<BillItem> items = orders.stream()
                .map(o -> new BillItem(o.getId(), o.getItemName(), o.getPrice(), o.getStatus()))
                .collect(Collectors.toList());

        int servedTotal = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.SERVED)
                .mapToInt(o -> o.getPrice() != null ? o.getPrice() : 0)
                .sum();

        int pendingTotal = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.SERVED)
                .mapToInt(o -> o.getPrice() != null ? o.getPrice() : 0)
                .sum();

        return new BillResponse(
                session.getSessionUuid(),
                session.getTableId(),
                session.getCheckedInAt(),
                LocalDateTime.now(),
                items,
                servedTotal,
                pendingTotal,
                session.getStatus().name()
        );
    }

    /**
     * Builds an itemized bill for the given session.
     * Read-only — safe to call multiple times for bill preview.
     */
    @Transactional(readOnly = true)
    public BillResponse generateBill(String sessionUuid) {
        CustomerSession session = sessionRepository.findBySessionUuid(sessionUuid)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionUuid));

        // C3 fix: direct call to private helper — NOT a self-proxy call
        return buildBillInternal(session);
    }

    /**
     * Finalises the session: marks CHECKED_OUT, returns the final bill.
     * Idempotent — safe to call multiple times if connection drops.
     */
    @Transactional
    public BillResponse checkout(String sessionUuid) {
        CustomerSession session = sessionRepository.findBySessionUuid(sessionUuid)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionUuid));

        if (session.getStatus() != SessionStatus.CHECKED_OUT) {
            session.setStatus(SessionStatus.CHECKED_OUT);
            sessionRepository.save(session);
        }

        // C3 fix: call private helper directly — avoids @Transactional(readOnly) being silently
        // bypassed when invoked via same-bean reference (Spring AOP proxy not traversed)
        BillResponse bill = buildBillInternal(session);

        log.info("[CHECKOUT] Session {}… (Table {}) checked out. Served total: ₹{}",
                session.getSessionUuid().substring(0, 8),
                session.getTableId(),
                bill.getServedTotal());

        return bill;
    }
}
