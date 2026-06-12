package com.dinesync.dinesync.service;

import com.dinesync.dinesync.model.Order;
import com.dinesync.dinesync.model.OrderMessage;
import com.dinesync.dinesync.model.OrderStatus;
import com.dinesync.dinesync.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SessionService sessionService;

    /**
     * C1 fix: server-side canonical price lookup.
     * The client-supplied price is IGNORED entirely.
     * Any item name not in this map is rejected as an unknown order.
     *
     * W3 fix: prices are stored as WHOLE RUPEES (not paise).
     * If integrating with Razorpay/Stripe (which use paise), multiply by 100 at the gateway layer.
     */
    private static final Map<String, Integer> MENU_PRICES = Map.of(
        "Butter Chicken", 280,
        "Paneer Tikka",   220,
        "Garlic Naan",    60,
        "Dal Makhani",    180,
        "Biryani",        320,
        "Masala Cola",    80,
        "Mango Lassi",    90,
        "Beer",           150
    );

    /**
     * Phase 2+: Validates the session, looks up the canonical server-side price,
     * persists a new order, bumps session activity, and returns the saved entity.
     *
     * @throws IllegalArgumentException if the item name is not in MENU_PRICES
     */
    @Transactional
    public Order createOrder(OrderMessage orderMsg) {
        // C1 fix: look up price on the server — never trust the client-supplied value
        Integer canonicalPrice = MENU_PRICES.get(orderMsg.getItem());
        if (canonicalPrice == null) {
            throw new IllegalArgumentException(
                "Unknown menu item: '" + orderMsg.getItem() + "'. Order rejected.");
        }

        String sessionUuid = sessionService.extractSessionUuid(orderMsg.getSessionId());

        Order order = new Order();
        order.setSessionUuid(sessionUuid);
        order.setTableId(orderMsg.getTable());
        order.setItemName(orderMsg.getItem());
        order.setPrice(canonicalPrice);          // server-authoritative price
        order.setStatus(OrderStatus.RECEIVED);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        Order saved = orderRepository.save(order);

        sessionService.updateLastActivity(sessionUuid);

        log.info("[ORDER #{}] Table {} ordered: {} (₹{})",
                saved.getId(), orderMsg.getTable(), orderMsg.getItem(), canonicalPrice);
        return saved;
    }

    /**
     * Phase 3: Updates the status of an existing order atomically.
     *
     * @Transactional ensures findById + save are one DB transaction (C2 fix from Phase 3 review).
     * Forward-only transitions enforced (W1 fix from Phase 3 review).
     */
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (newStatus.ordinal() <= order.getStatus().ordinal()) {
            throw new IllegalArgumentException(
                "Cannot move order #" + orderId + " from " + order.getStatus()
                + " back to " + newStatus + ". Transitions are one-way: RECEIVED → PREPARING → SERVED");
        }

        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());

        Order updated = orderRepository.save(order);

        log.info("[STATUS] Order #{} → {} (Table {}, {})",
                orderId, newStatus, order.getTableId(), order.getItemName());
        return updated;
    }
}
