package com.example.slotbooking.client.stub;

import com.example.slotbooking.client.RouteClient;
import java.time.Duration;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
class StubRouteClient implements RouteClient {

    @Override
    public ArrivalWindow getEstimatedArrivalWindow(String warehouseCode, LocalDate date) {
        StubLatency.simulate(200);
        return new ArrivalWindow(Duration.ofMinutes(20));
    }
}
