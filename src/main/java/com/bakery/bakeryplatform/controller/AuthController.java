package com.bakery.bakeryplatform.controller;

import com.bakery.bakeryplatform.model.user.AdminUser;
import com.bakery.bakeryplatform.model.user.Customer;
import com.bakery.bakeryplatform.model.user.User;
import com.bakery.bakeryplatform.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegistrationRequest request,
            HttpServletRequest servletRequest) {
        if (userService.customerEmailExists(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }

        Customer customer = new Customer();
        customer.setUsername(request.fullName().trim());
        customer.setEmail(request.email().trim());
        customer.setPhoneNumber(request.phone().trim());
        customer.setPassword(request.password());
        customer = userService.register(customer);

        HttpSession session = newAuthenticatedSession(servletRequest);
        session.setAttribute("loggedInUserId", customer.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new AuthResponse(true, account(customer), safeReturnUrl(request.returnUrl(), "index.html"), null));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        AdminUser admin = userService.authenticateAdmin(request.email(), request.password()).orElse(null);
        if (admin != null) {
            HttpSession session = newAuthenticatedSession(servletRequest);
            session.setAttribute("loggedInAdminId", admin.getId());
            return new AuthResponse(true, new Account(admin.getId(), admin.getUsername(), admin.getEmail(), "ADMIN"),
                    "/admin.html", null);
        }

        User user = userService.authenticate(request.email(), request.password()).orElse(null);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }
        HttpSession session = newAuthenticatedSession(servletRequest);
        session.setAttribute("loggedInUserId", user.getId());
        return new AuthResponse(true, account(user), safeReturnUrl(request.returnUrl(), "index.html"), null);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    private static HttpSession newAuthenticatedSession(HttpServletRequest request) {
        HttpSession previous = request.getSession(false);
        if (previous != null) {
            previous.invalidate();
        }
        return request.getSession(true);
    }

    private static Account account(User user) {
        String fullName = user.getUsername();
        return new Account(user.getId(), fullName, user.getEmail(), user.getRole());
    }

    private static String safeReturnUrl(String requested, String fallback) {
        if (requested == null || requested.isBlank()) {
            return fallback;
        }
        String value = requested.trim();
        if (value.startsWith("//") || value.contains("\\") || value.contains(":")
                || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            return fallback;
        }
        return value;
    }

    public record RegistrationRequest(
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 5, max = 30) String phone,
            @NotBlank @Size(min = 6, max = 100) String password,
            @Size(max = 500) String returnUrl) {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 254) String email,
            @NotBlank @Size(max = 100) String password,
            @Size(max = 500) String returnUrl) {
    }

    public record AuthResponse(boolean success, Account user, String redirectUrl, String message) {
    }

    public record Account(Long id, String fullName, String email, String role) {
    }
}
