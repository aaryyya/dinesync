package com.dinesync.dinesync.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single line item on a customer's bill.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillItem {
    private Long        orderId;
    private String      itemName;
    private Integer     price;    // ₹ in rupees (matches CustomerView MENU_ITEMS)
    private OrderStatus status;
}
