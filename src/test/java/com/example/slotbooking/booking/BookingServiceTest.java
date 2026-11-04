package com.example.slotbooking.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.slotbooking.client.CustomerClient;
import com.example.slotbooking.client.CustomerClient.Customer;
import com.example.slotbooking.client.NotificationClient;
import com.example.slotbooking.client.OrderClient;
import com.example.slotbooking.client.OrderClient.Order;
import com.example.slotbooking.client.OrderClient.OrderStatus;
import com.example.slotbooking.client.RouteClient;
import com.example.slotbooking.client.RouteClient.ArrivalWindow;
import com.example.slotbooking.slot.Slot;
import com.example.slotbooking.slot.SlotRepository;
import com.example.slotbooking.warehouse.Warehouse;
import com.example.slotbooking.warehouse.WarehouseResolver;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private SlotRepository slotRepository;
    @Mock
    private OrderClient orderClient;
    @Mock
    private CustomerClient customerClient;
    @Mock
    private NotificationClient notificationClient;
    @Mock
    private RouteClient routeClient;
    @Mock
    private WarehouseResolver warehouseResolver;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void createsBookingAndDecrementsCapacity() {
        Slot slot = slot(1L, 5);
        when(orderClient.findOrder("order-1"))
                .thenReturn(Optional.of(new Order("order-1", "customer-1", "1012 AB", OrderStatus.OPEN)));
        when(bookingRepository.findActiveByOrderId("order-1")).thenReturn(Optional.empty());
        when(slotRepository.findById(1L)).thenReturn(Optional.of(slot));
        when(bookingRepository.insert("order-1", "customer-1", 1L, BookingState.ACTIVE))
                .thenReturn(new Booking(100L, "order-1", "customer-1", 1L, BookingState.ACTIVE));
        stubArrivalWindow();

        BookingResponse response = bookingService.createBooking("customer-1", "order-1", 1L);

        assertThat(response.bookingId()).isEqualTo(100L);
        assertThat(response.state()).isEqualTo(BookingState.ACTIVE);
        verify(slotRepository).updateRemainingCapacity(1L, 4);
    }

    @Test
    void rejectsFullSlot() {
        when(orderClient.findOrder("order-1"))
                .thenReturn(Optional.of(new Order("order-1", "customer-1", "1012 AB", OrderStatus.OPEN)));
        when(bookingRepository.findActiveByOrderId("order-1")).thenReturn(Optional.empty());
        when(slotRepository.findById(1L)).thenReturn(Optional.of(slot(1L, 0)));

        assertThatThrownBy(() -> bookingService.createBooking("customer-1", "order-1", 1L))
                .isInstanceOf(RuntimeException.class);
        verify(slotRepository, never()).updateRemainingCapacity(anyLong(), anyInt());
    }

    @Test
    void reschedulesBookingToNewSlot() {
        Booking booking = new Booking(100L, "order-1", "customer-1", 1L, BookingState.ACTIVE);
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(slotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(slot(1L, 5)));
        when(slotRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(slot(2L, 3)));
        when(customerClient.getCustomer("customer-1")).thenReturn(new Customer("customer-1", "jane@example.com"));
        stubArrivalWindow();

        bookingService.reschedule(100L, 2L, "customer-1");

        Booking updated = bookingRepository.findById(100L).orElseThrow();
        assertThat(updated.state()).isEqualTo(BookingState.ACTIVE);
        verify(slotRepository).updateRemainingCapacity(1L, 6);
        verify(slotRepository).updateRemainingCapacity(2L, 2);
        verify(bookingRepository).updateSlot(100L, 2L);
    }

    private void stubArrivalWindow() {
        when(warehouseResolver.resolve(any())).thenReturn(new Warehouse("NL-AMS", ZoneOffset.ofHours(2)));
        when(routeClient.getEstimatedArrivalWindow(eq("NL-AMS"), any()))
                .thenReturn(new ArrivalWindow(Duration.ofMinutes(20)));
    }

    private static Slot slot(long id, int remainingCapacity) {
        Instant start = Instant.now().plus(3, ChronoUnit.HOURS);
        return new Slot(id, "1012 AB", start, start.plus(1, ChronoUnit.HOURS), 10, remainingCapacity);
    }
}
