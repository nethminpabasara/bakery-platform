package com.bakery.bakeryplatform.controller.website;

import com.bakery.bakeryplatform.service.cake.SiteBookings;
import com.bakery.bakeryplatform.service.order.SiteOrders;
import com.bakery.bakeryplatform.service.user.SiteIdentity;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
public class SiteFormController {
    private final SiteOrders orders;
    private final SiteBookings bookings;

    public SiteFormController(SiteOrders orders, SiteBookings bookings) {
        this.orders = orders;
        this.bookings = bookings;
    }

    @PostMapping("/website/order")
    public String checkout(@RequestParam Map<String, String> form, HttpSession session) {
        String action = form.getOrDefault("action", "create");
        if (action.equals("cancel") || action.equals("delete")) {
            Long orderId = SiteIdentity.id(form.get("orderId"));
            orders.cancel(orders.owned(orderId, session));
            return "redirect:/orders.html";
        }
        Long paymentId = orders.checkout(form, session);
        return "redirect:/order-confirmation.html?paymentId=" + paymentId;
    }

    @PostMapping("/website/cake-booking")
    public String saveBooking(@RequestParam Map<String, String> form, HttpSession session) {
        String action = form.getOrDefault("action", "create");
        if (action.equals("cancel") || action.equals("delete")) {
            bookings.cancel(SiteIdentity.id(form.get("bookingId")), session);
        } else {
            bookings.save(form, null, session);
        }
        return "redirect:/orders.html?tab=bookings&msg=Booking%20saved";
    }
}
