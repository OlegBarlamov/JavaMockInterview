package com.example.slotbooking.warehouse;

import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WarehouseResolver {

    private static final Warehouse AMSTERDAM = new Warehouse("NL-AMS", ZoneOffset.ofHours(2));
    private static final Warehouse BERLIN = new Warehouse("DE-Ber", ZoneOffset.ofHours(2));
    private static final Warehouse PARIS = new Warehouse("FR-PAR", ZoneOffset.ofHours(2));

    // Postcode prefix -> warehouse. TODO: add London when UK launches
    private static final Map<String, Warehouse> WAREHOUSES_BY_PREFIX = Map.of(
            "10", AMSTERDAM,
            "11", AMSTERDAM,
            "12", BERLIN,
            "13", BERLIN,
            "14", BERLIN,
            "75", PARIS);

    public Warehouse resolve(String postcode) {
        return WAREHOUSES_BY_PREFIX.get(postcode.substring(0, 2));
    }
}
