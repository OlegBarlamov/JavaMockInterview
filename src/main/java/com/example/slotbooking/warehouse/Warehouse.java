package com.example.slotbooking.warehouse;

import java.time.ZoneOffset;

public record Warehouse(String code, ZoneOffset utcOffset) {
}
