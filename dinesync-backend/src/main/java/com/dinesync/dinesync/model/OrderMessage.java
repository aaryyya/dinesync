package com.dinesync.dinesync.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessage {
    private Long    orderId;     // Set by backend after persisting; null on first broadcast to customer
    private Integer table;       // N2 fix: Integer (boxed) so missing field deserialises as null, not 0
    private String  item;
    private String  sessionId;   // JWT — validated by OrderController, NEVER broadcast to clients
    private String  status;      // Used for status-update pushes (Phase 3+)
    private Integer price;       // Phase 4: stored in DB for billing; sent by customer at order time
}
