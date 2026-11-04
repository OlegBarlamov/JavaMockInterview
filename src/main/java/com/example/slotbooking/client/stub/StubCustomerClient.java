package com.example.slotbooking.client.stub;

import com.example.slotbooking.client.CustomerClient;
import org.springframework.stereotype.Component;

@Component
class StubCustomerClient implements CustomerClient {

    @Override
    public Customer getCustomer(String customerId) {
        StubLatency.simulate(50);
        return new Customer(customerId, customerId + "@example.com");
    }
}
