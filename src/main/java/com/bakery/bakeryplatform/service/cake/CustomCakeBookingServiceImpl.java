package com.bakery.bakeryplatform.service.cake;

import com.bakery.bakeryplatform.model.cake.BookingStatus;
import com.bakery.bakeryplatform.model.cake.CustomCakeBooking;
import com.bakery.bakeryplatform.repository.cake.CustomCakeBookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomCakeBookingServiceImpl implements BookingService {

    @Autowired
    private CustomCakeBookingRepository repository;

    @Override
    public CustomCakeBooking createBooking(CustomCakeBooking booking) {
        booking.setStatus(BookingStatus.PENDING);
        return repository.save(booking);
    }

    @Override
    public List<CustomCakeBooking> getAllBookings() {
        return repository.findAll();
    }

    @Override
    public CustomCakeBooking getBookingById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public CustomCakeBooking updateBookingStatus(Long id, String status) {
        CustomCakeBooking booking = getBookingById(id);
        if (booking != null) {
            booking.setStatus(BookingStatus.valueOf(status.toUpperCase()));
            return repository.save(booking);
        }
        return null;
    }

    @Override
    public void deleteBooking(Long id) {
        repository.deleteById(id);
    }
}