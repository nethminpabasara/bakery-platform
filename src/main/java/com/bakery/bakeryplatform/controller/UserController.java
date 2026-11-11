package com.bakery.bakeryplatform.controller;

import com.bakery.bakeryplatform.model.user.AdminUser;
import com.bakery.bakeryplatform.model.user.Customer;
import com.bakery.bakeryplatform.model.user.User;
import com.bakery.bakeryplatform.service.user.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping({"/register", "/signup"})
    public String registerUser(@RequestParam String fullName,
                               @RequestParam String email,
                               @RequestParam String phone,
                               @RequestParam String password) {
        Customer customer = new Customer();
        customer.setUsername(fullName.trim());
        customer.setEmail(email.trim());
        customer.setPhoneNumber(phone.trim());
        customer.setPassword(password);
        userService.register(customer);
        return "redirect:/login";
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam("email") String username,
                            @RequestParam String password,
                            @RequestParam(required = false) String returnUrl,
                            HttpSession session, Model model) {
        Optional<AdminUser> admin = userService.authenticateAdmin(username, password);
        if (admin.isPresent()) {
            session.removeAttribute("loggedInUserId");
            session.setAttribute("loggedInAdminId", admin.get().getId());
            return "redirect:/admin.html";
        }

        Optional<User> user = userService.authenticate(username, password);
        if (user.isPresent()) {
            session.removeAttribute("loggedInAdminId");
            session.setAttribute("loggedInUserId", user.get().getId());
            return "redirect:" + safeReturnUrl(returnUrl);
        }

        return "redirect:/login?error=invalid_credentials";
    }

    @GetMapping({"/profile", "/profile.html"})
    public String showMyProfile(HttpSession session, Model model) {
        if (session.getAttribute("loggedInAdminId") != null) {
            return "redirect:/admin/profile";
        }
        Long userId = (Long) session.getAttribute("loggedInUserId");
        if (userId == null) {
            return "redirect:/login";
        }

        return "redirect:/account.html";
    }

    @GetMapping("/api/profile")
    @ResponseBody
    public ResponseEntity<ProfileResponse> getMyProfile(HttpSession session) {
        Long userId = (Long) session.getAttribute("loggedInUserId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userService.findById(userId);
        if (!(user instanceof Customer customer)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(new ProfileResponse(
                customer.getUsername(),
                customer.getEmail(),
                customer.getPhoneNumber(),
                customer.getAddress(),
                customer.getRole()));
    }

    @PutMapping("/api/profile")
    @ResponseBody
    public ResponseEntity<ProfileResponse> updateMyProfile(
            @RequestBody ProfileUpdateRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute("loggedInUserId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Customer updates = new Customer();
        updates.setUsername(request.fullName());
        updates.setPhoneNumber(request.phoneNumber());
        updates.setAddress(request.address());
        updates.setPassword(request.password());
        userService.updateProfile(userId, updates);

        User user = userService.findById(userId);
        if (!(user instanceof Customer customer)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(new ProfileResponse(
                customer.getUsername(),
                customer.getEmail(),
                customer.getPhoneNumber(),
                customer.getAddress(),
                customer.getRole()));
    }

    @PostMapping("/profile/update")
    public String updateMyProfile(@ModelAttribute Customer customer, HttpSession session) {
        Long userId = (Long) session.getAttribute("loggedInUserId");
        if (userId == null) {
            return "redirect:/login?returnUrl=%2Fprofile";
        }
        userService.updateProfile(userId, customer);
        return "redirect:/profile";
    }

    @GetMapping("/profile/delete")
    public String deleteMyProfile(HttpSession session) {
        Long userId = (Long) session.getAttribute("loggedInUserId");
        if (userId != null) {
            userService.deleteById(userId);
            session.invalidate();
        }
        return "redirect:/login.html#signup";
    }

    @GetMapping({"/admin/users", "/adminuserlist.html"})
    public String showUserList(HttpSession session, Model model) {
        if (session.getAttribute("loggedInAdminId") == null) {
            return "redirect:/login";
        }
        return "redirect:/admin.html?tab=users";
    }

    @GetMapping("/admin/user/delete/{id}")
    public String deleteUser(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("loggedInAdminId") == null) {
            return "redirect:/login";
        }
        userService.deleteById(id);
        return "redirect:/admin.html?tab=users";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login.html";
    }

    public record ProfileResponse(String fullName, String email, String phoneNumber, String address, String role) {
    }

    public record ProfileUpdateRequest(String fullName, String phoneNumber, String address, String password) {
    }

    private static String safeReturnUrl(String requested) {
        if (requested == null || requested.isBlank()) return "/index.html";
        String value = requested.trim();
        return value.startsWith("/") && !value.startsWith("//")
                && !value.contains("\\") && !value.contains("://")
                ? value
                : "/index.html";
    }
}
