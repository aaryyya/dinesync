package com.dinesync.dinesync.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_uuid", nullable = false, length = 36)
    private String sessionUuid;

    @Column(name = "table_id", nullable = false)
    private Integer tableId;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    /**
     * Price in WHOLE RUPEES at time of order (e.g. 280 for ₹280).
     * W3 fix: documented consistently as rupees — matches frontend MENU_ITEMS and billing output.
     * Note: if integrating with Razorpay/Stripe (paise), multiply by 100 at the gateway layer.
     * Nullable for backward compatibility with Phase 1/2 orders created before this column existed.
     */
    @Column(name = "price")
    private Integer price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
