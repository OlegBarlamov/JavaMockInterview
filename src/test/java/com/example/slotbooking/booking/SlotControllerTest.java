package com.example.slotbooking.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.example.slotbooking.booking.SlotBookingRepository.Slot;
import com.example.slotbooking.booking.SlotController.SlotResponse;
import com.example.slotbooking.client.RouteClient;
import com.example.slotbooking.client.RouteClient.ArrivalWindow;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SlotControllerTest {

    @Mock
    private SlotBookingRepository repository;
    @Mock
    private RouteClient routeClient;

    @InjectMocks
    private SlotController controller;

    @Test
    void returnsAvailableSlotsWithArrivalWindow() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Instant start = tomorrow.atTime(10, 0).toInstant(ZoneOffset.ofHours(2));
        when(repository.findAvailableSlots(eq("1012 AB"), any(), any()))
                .thenReturn(List.of(new Slot(1L, "1012 AB", start, start.plus(1, ChronoUnit.HOURS), 10, 5)));
        when(routeClient.getEstimatedArrivalWindow("NL-AMS", tomorrow))
                .thenReturn(new ArrivalWindow(Duration.ofMinutes(20)));

        List<SlotResponse> slots = controller.listSlots("1012 AB", tomorrow);

        assertThat(slots).hasSize(1);
        assertThat(slots.getFirst().estimatedArrivalWindowMinutes()).isEqualTo(20);
    }

    @Test
    void filtersOutSlotsPastCutoff() {
        LocalDate today = LocalDate.now();
        Instant soon = Instant.now().plus(1, ChronoUnit.HOURS);
        when(repository.findAvailableSlots(eq("1012 AB"), any(), any()))
                .thenReturn(List.of(new Slot(1L, "1012 AB", soon, soon.plus(1, ChronoUnit.HOURS), 10, 5)));

        assertThat(controller.listSlots("1012 AB", today)).isEmpty();
    }
}
