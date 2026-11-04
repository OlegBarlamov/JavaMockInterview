package com.example.slotbooking.booking;

import com.example.slotbooking.auth.CustomerPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public BookingResponse book(@RequestBody BookingRequest request) {
        return bookingService.book(request);
    }

    @PutMapping("/{id}")
    public BookingResponse reschedule(
            @PathVariable long id,
            @RequestBody RescheduleRequest request,
            CustomerPrincipal principal) {
        return bookingService.reschedule(id, request.newSlotId(), principal.customerId());
    }
}
