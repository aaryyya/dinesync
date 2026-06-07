package com.dinesync.dinesync.controller;

import com.dinesync.dinesync.model.OrderMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class OrderController {

    @MessageMapping("/order")
    @SendTo("/topic/kitchen")
    public OrderMessage sendOrder(OrderMessage order) {

        System.out.println(
                "Order Received: Table "
                        + order.getTable()
                        + " Item: "
                        + order.getItem());

        return order;
    }
}