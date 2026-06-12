package com.dinesync.dinesync.controller;

import com.dinesync.dinesync.model.Order;
import com.dinesync.dinesync.model.OrderMessage;
import com.dinesync.dinesync.model.OrderStatus;
import com.dinesync.dinesync.service.OrderService;
import com.dinesync.dinesync.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
public class OrderController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private OrderService orderService;

    @MessageMapping("/order")
    public void sendOrder(OrderMessage orderMsg) {
        // Guard 1: reject orders without a valid, ACTIVE session JWT
        if (!sessionService.isValidSession(orderMsg.getSessionId())) {
            log.warn("[REJECTED] Invalid session on order request");
            messagingTemplate.convertAndSend("/topic/errors",
                "{\"error\": \"Invalid session. Please scan the QR code to check in.\"}");
            return;
        }

        // Guard 2: delegate to service; reject if item not on the menu (C1 fix)
        Order saved;
        try {
            saved = orderService.createOrder(orderMsg);
        } catch (IllegalArgumentException e) {
            log.warn("[REJECTED] {}", e.getMessage());
            messagingTemplate.convertAndSend("/topic/errors",
                "{\"error\": \"" + e.getMessage() + "\"}");
            return;
        }

        // Build kitchen broadcast — sessionId and client-supplied price NEVER broadcast
        OrderMessage broadcast = new OrderMessage();
        broadcast.setOrderId(saved.getId());
        broadcast.setTable(orderMsg.getTable());
        broadcast.setItem(orderMsg.getItem());
        broadcast.setPrice(saved.getPrice());
        broadcast.setStatus(OrderStatus.RECEIVED.name());

        messagingTemplate.convertAndSend("/topic/kitchen", broadcast);

        // ROOT BUG FIX: also push to the customer's own session topic so the
        // CustomerView can seed the real DB orderId into its local state.
        //
        // Without this, sentOrders has { orderId: null } and the customer's
        // connectSession callback can never match incoming PREPARING / SERVED
        // updates (which arrive with the real orderId from the DB).
        //
        // With this push, the RECEIVED message triggers an item-name match that
        // upgrades the null orderId to the real one — all subsequent status pushes
        // then match correctly by orderId.
        messagingTemplate.convertAndSend("/topic/session/" + saved.getSessionUuid(), broadcast);
    }
}