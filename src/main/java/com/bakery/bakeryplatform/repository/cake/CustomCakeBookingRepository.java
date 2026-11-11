package com.bakery.bakeryplatform.repository.cake;

import com.bakery.bakeryplatform.model.cake.CustomCakeBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomCakeBookingRepository extends JpaRepository<CustomCakeBooking, Long> {
}