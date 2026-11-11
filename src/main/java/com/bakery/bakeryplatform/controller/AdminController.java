package com.bakery.bakeryplatform.controller;

import com.bakery.bakeryplatform.model.user.AdminUser;
import com.bakery.bakeryplatform.service.user.DeliveryService;
import com.bakery.bakeryplatform.service.user.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class AdminController {

    private final UserService userService;
    private final DeliveryService deliveryService;

    public AdminController(UserService userService, DeliveryService deliveryService) {
        this.userService = userService;
        this.deliveryService = deliveryService;
    }

    private AdminUser currentAdmin(HttpSession session) {
        Long adminId = (Long) session.getAttribute("loggedInAdminId");
        return adminId == null ? null : userService.findAdminById(adminId);
    }

    /** Open the dashboard tab for user or delivery management. */
    @GetMapping({"/admin/profile", "/adminprofile.html"})
    public String adminProfile(@RequestParam(defaultValue = "users") String tab, HttpSession session) {
        AdminUser admin = currentAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }
        return "redirect:/admin.html?tab=" + ("deliveries".equals(tab) ? "orders" : "users");
    }

    @PostMapping("/admin/delivery/update/{id}")
    public String updateDeliveryStatus(@PathVariable Long id, @RequestParam String status,
                                       HttpSession session) {
        if (currentAdmin(session) == null) {
            return "redirect:/login";
        }
        deliveryService.updateStatus(id, status);
        return "redirect:/admin/profile?tab=deliveries";
    }

    @GetMapping("/admin/delivery/delete/{id}")
    public String deleteDelivery(@PathVariable Long id, HttpSession session) {
        if (currentAdmin(session) == null) {
            return "redirect:/login";
        }
        deliveryService.deleteById(id);
        return "redirect:/admin/profile?tab=deliveries";
    }
}
