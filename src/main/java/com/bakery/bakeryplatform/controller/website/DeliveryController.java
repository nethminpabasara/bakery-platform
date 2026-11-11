package com.bakery.bakeryplatform.controller.website;

import com.bakery.bakeryplatform.service.order.SiteOrders;
import com.bakery.bakeryplatform.service.user.SiteIdentity;

import com.bakery.bakeryplatform.model.user.Customer;
import com.bakery.bakeryplatform.model.user.Delivery;
import com.bakery.bakeryplatform.repository.user.DeliveryRepository;
import com.bakery.bakeryplatform.service.user.DeliveryService;
import com.bakery.bakeryplatform.service.user.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.bakery.bakeryplatform.service.user.SiteIdentity.error;
import static com.bakery.bakeryplatform.service.user.SiteIdentity.map;

@Controller
public class DeliveryController {
    private static final Set<String> STATUSES =
            Set.of("ASSIGNED", "PENDING", "OUT_FOR_DELIVERY", "ON_THE_WAY", "DELIVERED", "CANCELLED");

    private final DeliveryService deliveryService;
    private final UserService userService;
    private final DeliveryRepository deliveries;
    private final SiteIdentity identity;
    private final SiteOrders orders;

    public DeliveryController(DeliveryService deliveryService, UserService userService,
                              DeliveryRepository deliveries, SiteIdentity identity, SiteOrders orders) {
        this.deliveryService = deliveryService;
        this.userService = userService;
        this.deliveries = deliveries;
        this.identity = identity;
        this.orders = orders;
    }

    @GetMapping({"/delivery/status", "/delivery/info", "/delivery"})
    public String showDeliveryStatus(HttpSession session) {
        if (identity.admin(session)) {
            return "redirect:/admin.html?tab=orders";
        }
        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }
        return "redirect:/account.html#deliveries";
    }

    @GetMapping("/delivery/new")
    public String showDeliveryForm() {
        return "redirect:/bakery.html?action=checkout";
    }

    @PostMapping("/delivery/new")
    public String createDelivery(@ModelAttribute Delivery delivery, HttpSession session) {
        Long userId = (Long) session.getAttribute("loggedInUserId");
        if (userId == null) {
            return "redirect:/login";
        }

        if (userService.findById(userId) instanceof Customer customer) {
            delivery.setCustomer(customer);
            if (customer.getAddress() != null && !customer.getAddress().isBlank()) {
                delivery.setAddress(customer.getAddress());
            }
            if (delivery.getRecipientName() == null || delivery.getRecipientName().isBlank()) {
                delivery.setRecipientName(customer.getUsername());
            }
        }
        deliveryService.create(delivery);
        return "redirect:/delivery/status";
    }

    @PostMapping("/delivery/update/{id}")
    public String updateDeliveryStatus(@PathVariable Long id, @RequestParam String status) {
        deliveryService.updateStatus(id, status);
        return "redirect:/delivery/status";
    }

    @GetMapping("/delivery/delete/{id}")
    public String deleteDelivery(@PathVariable Long id) {
        deliveryService.deleteById(id);
        return "redirect:/delivery/status";
    }

    @GetMapping("/api/site/deliveries")
    @ResponseBody
    public List<Map<String, Object>> deliveries(HttpSession session) {
        identity.requireAdmin(session);
        return deliveries.findAll().stream().map(DeliveryController::summary).toList();
    }

    @GetMapping("/api/site/deliveries/order/{id}")
    @ResponseBody
    public Map<String, Object> delivery(@PathVariable Long id, HttpSession session) {
        orders.owned(id, session);
        return deliveries.findFirstByOrderId(id).map(DeliveryController::summary).orElseGet(SiteIdentity::map);
    }

    @RequestMapping(value = "/api/site/deliveries/{id}/status", method = {RequestMethod.PUT, RequestMethod.POST})
    @ResponseBody
    public Map<String, Object> deliveryStatus(@PathVariable Long id, @RequestParam String status, HttpSession session) {
        identity.requireAdmin(session);
        if (!STATUSES.contains(status)) {
            throw error(HttpStatus.BAD_REQUEST, "Invalid delivery status.");
        }
        Delivery delivery = deliveries.findById(id)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Delivery not found."));
        delivery.setStatus(status);
        return summary(deliveries.save(delivery));
    }

    private static Map<String, Object> summary(Delivery delivery) {
        return map("id", delivery.getId(), "deliveryId", delivery.getId().toString(),
                "orderId", delivery.getOrderId() == null ? null : delivery.getOrderId().toString(),
                "deliveryAddress", delivery.getAddress(), "deliveryDate", delivery.getDeliveryDate(),
                "deliveryStatus", delivery.getStatus());
    }
}
