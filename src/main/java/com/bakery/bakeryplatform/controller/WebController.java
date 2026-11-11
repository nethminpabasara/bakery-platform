package com.bakery.bakeryplatform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class WebController {

    @GetMapping({"/", "/main"})
    public String home() {
        return "redirect:/index.html";
    }

    @GetMapping({"/custom-cake", "/custome_cake", "/items", "/item", "/item.html"})
    public String customCake() {
        return "redirect:/custom-cake.html";
    }

    @GetMapping({"/login", "/loging", "/loging.html"})
    public RedirectView login() {
        RedirectView login = new RedirectView("/login.html");
        login.setPropagateQueryParams(true);
        return login;
    }

    @GetMapping({"/register", "/register.html", "/singup", "/singup.html", "/signup"})
    public String register() {
        return "redirect:/login.html#signup";
    }

    @GetMapping({"/orders", "/my-orders", "/payment", "/payment.html", "/payments", "/payments.html"})
    public String orders() {
        return "redirect:/orders.html";
    }

    @GetMapping("/my-bookings")
    public String bookings() {
        return "redirect:/orders.html?tab=bookings";
    }

    @GetMapping({"/checkout", "/checkout.html"})
    public String checkout() {
        return "redirect:/bakery.html?action=checkout";
    }

    @GetMapping({"/profileupdate", "/profileupdate.html"})
    public String profileUpdate() {
        return "redirect:/account.html";
    }

    @GetMapping({"/invoice-detail", "/invoice-details", "/billing/invoice-detail.html"})
    public String invoiceDetails(
            @RequestParam(name = "id", required = false) Long id,
            @RequestParam(name = "invoiceId", required = false) Long invoiceId) {
        Long requestedId = id != null ? id : invoiceId;
        if (requestedId == null || requestedId < 1) {
            return "redirect:/orders.html?tab=invoices";
        }
        return "redirect:/orders.html?tab=invoices&invoiceId=" + requestedId;
    }

    @GetMapping({"/my-invoices", "/my-invoice", "/billing/my-invoices.html"})
    public String myInvoice() {
        return "redirect:/orders.html?tab=invoices";
    }

    @GetMapping({"/refund-request-form", "/billing/refund-request-form.html"})
    public String refundRequestForm(@RequestParam(name = "orderId", required = false) Long orderId) {
        if (orderId == null || orderId < 1) {
            return "redirect:/orders.html?tab=refunds";
        }
        return "redirect:/orders.html?tab=refunds&orderId=" + orderId;
    }

}
