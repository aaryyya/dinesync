package com.dinesync.dinesync.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for PUT /api/orders/{orderId}/status.
 *
 * W3 fix: use the OrderStatus enum type directly — Jackson rejects unknown values
 * at deserialization time with a clean 400, no service-layer try/catch needed.
 * C3 fix: @NotNull prevents null from bypassing the enum parser silently.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusRequest {

    @NotNull(message = "status must not be null. Valid values: RECEIVED, PREPARING, SERVED")
    private OrderStatus status;
}
