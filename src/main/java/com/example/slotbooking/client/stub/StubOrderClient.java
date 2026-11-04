package com.example.slotbooking.client.stub;

import com.example.slotbooking.client.OrderClient;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class StubOrderClient implements OrderClient {

    private static final Map<String, Order> ORDERS = Map.of(
            "order-1", new Order("order-1", "customer-1", "1012 AB", OrderStatus.OPEN),
            "order-2", new Order("order-2", "customer-2", "10115", OrderStatus.OPEN),
            "order-3", new Order("order-3", "customer-3", "75001", OrderStatus.OPEN));

    @Override
    public Optional<Order> findOrder(String orderId) {
        StubLatency.simulate(50);
        return Optional.ofNullable(ORDERS.get(orderId));
    }
}
