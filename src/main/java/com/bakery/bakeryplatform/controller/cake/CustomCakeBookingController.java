package com.bakery.bakeryplatform.controller.cake;

import com.bakery.bakeryplatform.model.cake.BookingStatus;
import com.bakery.bakeryplatform.model.cake.CustomCakeBooking;
import com.bakery.bakeryplatform.service.cake.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CustomCakeBookingController {

    @Autowired
    private BookingService bookingService;

    // Create a new booking
    @PostMapping("/bookings")
    public ResponseEntity<CustomCakeBooking> createBooking(@RequestBody CustomCakeBooking booking) {
        if (booking.getCustomerName() == null || booking.getCustomerName().isBlank()
                || booking.getContactNumber() == null || booking.getContactNumber().isBlank()
                || booking.getRequiredDate() == null || booking.getRequiredDate().isBlank()
                || booking.getCustomerEmail() == null || booking.getCustomerEmail().isBlank()
                || !Double.isFinite(booking.getEstimatedPrice()) || booking.getEstimatedPrice() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Complete the required booking details.");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(booking));
    }

    @GetMapping("/cakeBookings")
    public List<CustomCakeBooking> getAllBookings() {
        return bookingService.getAllBookings();
    }

    @GetMapping("/bookings/my")
    public List<CustomCakeBooking> getMyBookings() {
        return bookingService.getAllBookings();
    }

    @PostMapping("/admin/updateBookingStatus")
    public ResponseEntity<CustomCakeBooking> updateBookingStatus(@RequestBody Map<String, Object> request) {
        Object bookingIdValue = request.get("bookingId");
        Object statusValue = request.get("status");
        if (bookingIdValue == null || !(statusValue instanceof String statusText) || statusText.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking ID and status are required.");
        }

        Long bookingId;
        try {
            bookingId = Long.valueOf(bookingIdValue.toString());
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking ID must be a number.", exception);
        }

        BookingStatus status;
        try {
            status = BookingStatus.valueOf(statusText.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The booking status is invalid.", exception);
        }

        CustomCakeBooking booking = bookingService.updateBookingStatus(bookingId, status.name());
        if (booking == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The booking was not found.");
        }
        return ResponseEntity.ok(booking);
    }

    @DeleteMapping("/cakeBookings/{id}")
    public void deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
    }
}