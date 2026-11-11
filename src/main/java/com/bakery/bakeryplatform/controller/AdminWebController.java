package com.bakery.bakeryplatform.controller;

import com.bakery.bakeryplatform.model.user.Customer;
import com.bakery.bakeryplatform.model.user.User;
import com.bakery.bakeryplatform.service.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;


@Controller
public class AdminWebController {
    private final UserService userService;

    public AdminWebController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping({"/addItem", "/adminItems", "/adminitem", "/adminitem.html", "/adminpages/adminitem.html"})
    public String products() {
        return "redirect:/admin.html?tab=products";
    }

    @GetMapping({"/adminDashboard", "/adminindex.html", "/adminpages/adminindex.html"})
    public String dashboard() {
        return "redirect:/admin.html";
    }

    @GetMapping({"/customOrders", "/custome_order", "/custome_orders", "/custome_orders.html",
            "/adminpages/custome_orders.html"})
    public String bookings() {
        return "redirect:/admin.html?tab=bookings";
    }

    @GetMapping({"/bakeryOrders", "/admin-orders.html", "/adminpages/admin-orders.html",
            "/adminPayments", "/admin-payments.html", "/adminpages/admin-payments.html"})
    public String orders() {
        return "redirect:/admin.html?tab=orders";
    }

    @GetMapping({"/adminInvoices", "/adminpages/allinvoicess.html"})
    public String invoices() {
        return "redirect:/admin.html?tab=invoices";
    }

    @GetMapping({"/adminRefunds", "/admin-refund-management", "/adminpages/admin-refund-management.html"})
    public String refunds() {
        return "redirect:/admin.html?tab=refunds";
    }

    @GetMapping({"/users", "/userManagement", "/adminpages/userManage.html"})
    public String users() {
        return "redirect:/admin.html?tab=users";
    }

    @GetMapping("/api/admin/users")
    @ResponseBody
    public List<CustomerSummary> getCustomerUsers() {
        return userService.findAllCustomers().stream()
                .map(customer -> new CustomerSummary(
                        customer.getId(),
                        customer.getUsername(),
                        customer.getEmail(),
                        customer.getPhoneNumber()))
                .toList();
    }

    @DeleteMapping("/api/admin/users/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        User user = userService.findById(id);
        if (!(user instanceof Customer)) {
            return ResponseEntity.notFound().build();
        }
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    public record CustomerSummary(Long id, String username, String email, String phoneNumber) {
    }
}
