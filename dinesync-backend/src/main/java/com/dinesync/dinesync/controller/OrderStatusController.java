package com.dinesync.dinesync.controller;

import com.dinesync.dinesync.model.Order;
import com.dinesync.dinesync.model.OrderMessage;
import com.dinesync.dinesync.model.OrderStatusRequest;
import com.dinesync.dinesync.service.OrderService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * Phase 3: Kitchen Intelligence — order status lifecycle.
 *
 * C1 fix: protected by X-Kitchen-Secret header. Any request without the correct
 * secret is rejected with 403. This prevents customers from marking their own
 * orders as served via DevTools.
 *
 * N5 fix: @CrossOrigin removed — CorsConfig already covers /api/** globally.
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderStatusController {

    @Value("${kitchen.secret}")
    private String kitchenSecret;

    @Autowired
    private OrderService orderService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Updates an order's status and broadcasts the change to both:
     *   1. /topic/session/{sessionUuid}  — customer's phone (progress bar)
     *   2. /topic/kitchen               — kitchen board (row colour + label)
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderStatusRequest request,
            @RequestHeader(value = "X-Kitchen-Secret", required = false) String secret) {

        // C1 fix: reject requests without the correct kitchen secret
        if (!kitchenSecret.equals(secret)) {
            log.warn("Unauthorized status update attempt on order #{}", orderId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Forbidden: missing or invalid X-Kitchen-Secret header");
        }

        Order updated;
        try {
            updated = orderService.updateOrderStatus(orderId, request.getStatus());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        // Build lean WS payload — sessionId never broadcast
        OrderMessage statusMsg = new OrderMessage();
        statusMsg.setOrderId(updated.getId());
        statusMsg.setTable(updated.getTableId());
        statusMsg.setItem(updated.getItemName());
        statusMsg.setStatus(updated.getStatus().name());

        // ① Customer's phone: shows progress bar moving forward
        messagingTemplate.convertAndSend(
                "/topic/session/" + updated.getSessionUuid(), statusMsg);

        // ② Kitchen board: row colour + button state updates
        messagingTemplate.convertAndSend("/topic/kitchen", statusMsg);

        return ResponseEntity.ok(statusMsg);
    }
}
