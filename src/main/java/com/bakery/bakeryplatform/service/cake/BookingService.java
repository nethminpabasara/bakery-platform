package com.bakery.bakeryplatform.service.cake;

import com.bakery.bakeryplatform.model.cake.CustomCakeBooking;
import java.util.List;

public interface BookingService {
    CustomCakeBooking createBooking(CustomCakeBooking booking);
    List<CustomCakeBooking> getAllBookings();
    CustomCakeBooking getBookingById(Long id);
    CustomCakeBooking updateBookingStatus(Long id, String status);
    void deleteBooking(Long id);
}