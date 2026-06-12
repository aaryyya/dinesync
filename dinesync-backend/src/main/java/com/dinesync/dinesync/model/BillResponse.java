package com.dinesync.dinesync.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full billing summary for a customer session.
 * Returned by GET /api/sessions/{uuid}/bill and POST /api/sessions/{uuid}/checkout.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillResponse {

    private String        sessionUuid;
    private Integer       tableId;
    private LocalDateTime checkedInAt;
    private LocalDateTime billGeneratedAt;

    /** All orders for this session (RECEIVED + PREPARING + SERVED) */
    private List<BillItem> items;

    /** Sum of prices for SERVED orders — what the customer pays now */
    private int servedTotal;

    /** Sum of prices for still-in-progress orders — informational only */
    private int pendingTotal;

    /** Total of ALL orders, served or not */
    public int getGrandTotal() {
        return servedTotal + pendingTotal;
    }

    /** Session status at bill time (ACTIVE, CHECKED_OUT, EXPIRED) */
    private String sessionStatus;
}
