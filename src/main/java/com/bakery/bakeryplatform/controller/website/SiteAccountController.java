package com.bakery.bakeryplatform.controller.website;

import com.bakery.bakeryplatform.service.user.SiteIdentity;

import com.bakery.bakeryplatform.model.CartItem;
import com.bakery.bakeryplatform.repository.CartItemRepository;
import com.bakery.bakeryplatform.repository.order.OrderRepository;
import com.bakery.bakeryplatform.service.CartService;
import com.bakery.bakeryplatform.model.user.Customer;
import com.bakery.bakeryplatform.repository.user.AdminUserRepository;
import com.bakery.bakeryplatform.repository.user.UserRepository;
import com.bakery.bakeryplatform.service.user.UserService;
import com.bakery.bakeryplatform.controller.AuthController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.bakery.bakeryplatform.service.user.SiteIdentity.error;
import static com.bakery.bakeryplatform.service.user.SiteIdentity.id;
import static com.bakery.bakeryplatform.service.user.SiteIdentity.map;
import static com.bakery.bakeryplatform.service.user.SiteIdentity.text;

@RestController
@RequestMapping("/api/site")
public class SiteAccountController {
    private final AuthController auth;
    private final SiteIdentity identity;
    private final CartItemRepository carts;
    private final CartService cart;
    private final UserService userService;
    private final UserRepository users;
    private final AdminUserRepository admins;
    private final OrderRepository orders;

    public SiteAccountController(AuthController auth, SiteIdentity identity, CartItemRepository carts,
                                 CartService cart, UserService userService, UserRepository users,
                                 AdminUserRepository admins, OrderRepository orders) {
        this.auth = auth;
        this.identity = identity;
        this.carts = carts;
        this.cart = cart;
        this.userService = userService;
        this.users = users;
        this.admins = admins;
        this.orders = orders;
    }

    @PostMapping("/auth/login")
    @Transactional
    public Map<String, Object> login(@Valid @RequestBody AuthController.LoginRequest input, HttpServletRequest request) {
        Long guest = guestCartId(request);
        var result = auth.login(input, request);
        migrateGuestCart(guest, request.getSession());
        String redirect = result.user().role().equals("ADMIN") ? "/admin.html" : result.redirectUrl();
        return map("success", true, "user", session(request.getSession()).get("user"), "redirectUrl", redirect);
    }

    @PostMapping("/auth/register")
    @Transactional
    public Map<String, Object> register(@Valid @RequestBody AuthController.RegistrationRequest input,
                                        HttpServletRequest request) {
        Long guest = guestCartId(request);
        var result = auth.register(input, request).getBody();
        migrateGuestCart(guest, request.getSession());
        return map("success", true, "user", session(request.getSession()).get("user"), "redirectUrl", result.redirectUrl());
    }

    @GetMapping({"/auth/me", "/auth/session", "/currentUser"})
    public Map<String, Object> session(HttpSession session) {
        Map<String, Object> user = null;
        if (identity.admin(session)) {
            Long adminId = (Long) session.getAttribute("loggedInAdminId");
            user = identity.administrator(admins.findById(adminId).orElseThrow());
        } else if (session.getAttribute("loggedInUserId") instanceof Long id
                && users.findById(id).orElse(null) instanceof Customer customer) {
            user = identity.user(customer);
        }
        var result = map("authenticated", user != null, "loggedIn", user != null, "user", user);
        if (user != null) {
            result.putAll(user);
        }
        return result;
    }

    @PostMapping("/auth/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        auth.logout(request);
        return map("success", true);
    }

    @PostMapping("/account/update")
    public Map<String, Object> profile(@RequestBody Map<String, Object> data, HttpSession session) {
        Customer customer = identity.customer(session);
        Customer values = new Customer();
        values.setUsername(text(data, "fullName", customer.getUsername()));
        values.setPhoneNumber(text(data, "phone", customer.getPhoneNumber()));
        values.setAddress(text(data, "address", customer.getAddress()));
        if (values.getUsername().isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "A name is required.");
        }
        userService.updateProfile(customer.getId(), values);
        return map("success", true);
    }

    @GetMapping("/users")
    public List<Map<String, Object>> listUsers(HttpSession session) {
        identity.requireAdmin(session);
        var result = new ArrayList<Map<String, Object>>();
        userService.findAllCustomers().forEach(customer -> result.add(identity.user(customer)));
        admins.findAll().forEach(admin -> result.add(identity.administrator(admin)));
        return result;
    }

    @PostMapping("/users/create")
    public Map<String, Object> createUser(@RequestBody Map<String, Object> data, HttpSession session) {
        identity.requireAdmin(session);
        String name = text(data, "fullName", "");
        String email = text(data, "email", "").toLowerCase();
        String password = text(data, "password", "");
        validateNameAndEmail(name, email);
        if (password.length() < 6) {
            throw error(HttpStatus.BAD_REQUEST, "Use a password with at least six characters.");
        }
        if (users.findByEmail(email) != null || admins.findByEmail(email) != null) {
            throw error(HttpStatus.CONFLICT, "This email is already registered.");
        }

        String role = text(data, "role", "CUSTOMER");
        if (role.equals("ADMIN")) {
            var admin = userService.createAdmin(email, email, password);
            admin.setUsername(name);
            admins.save(admin);
            return map("success", true, "user", identity.administrator(admin));
        }
        if (!role.equals("CUSTOMER")) {
            throw error(HttpStatus.BAD_REQUEST, "Choose CUSTOMER or ADMIN.");
        }

        var customer = new Customer();
        customer.setUsername(name);
        customer.setEmail(email);
        customer.setPassword(password);
        customer.setPhoneNumber(text(data, "phone", ""));
        customer.setAddress(text(data, "address", ""));
        return map("success", true, "user", identity.user(userService.register(customer)));
    }

    @PostMapping("/users/update")
    public Map<String, Object> updateUser(@RequestBody Map<String, Object> data, HttpSession session) {
        identity.requireAdmin(session);
        String key = text(data, "userId", "");
        String name = text(data, "fullName", "");
        String email = text(data, "email", "").toLowerCase();
        validateNameAndEmail(name, email);
        if (key.startsWith("ADMIN-")) {
            if (!text(data, "role", "ADMIN").equals("ADMIN")) {
                throw error(HttpStatus.CONFLICT, "Create a separate account to change roles.");
            }
            var admin = admins.findById(id(key.substring(6)))
                    .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Admin not found."));
            admin.setUsername(name);
            admin.setEmail(email);
            admins.save(admin);
        } else {
            if (!text(data, "role", "CUSTOMER").equals("CUSTOMER")) {
                throw error(HttpStatus.CONFLICT, "Create a separate administrator account to change roles.");
            }
            var user = users.findById(id(key))
                    .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Customer not found."));
            if (!(user instanceof Customer customer)) {
                throw error(HttpStatus.BAD_REQUEST, "Invalid customer.");
            }
            customer.setUsername(name);
            customer.setEmail(email);
            customer.setPhoneNumber(text(data, "phone", customer.getPhoneNumber()));
            users.save(customer);
        }
        return map("success", true);
    }

    @PostMapping("/users/delete")
    public Map<String, Object> deleteUser(@RequestBody Map<String, Object> data, HttpSession session) {
        identity.requireAdmin(session);
        String key = text(data, "userId", "");
        if (key.startsWith("ADMIN-")) {
            Long adminId = id(key.substring(6));
            if (adminId.equals(session.getAttribute("loggedInAdminId"))) {
                throw error(HttpStatus.CONFLICT, "You cannot delete your signed-in administrator account.");
            }
            admins.deleteById(adminId);
        } else {
            Long customerId = id(key);
            if (!orders.findByUserId(customerId).isEmpty()) {
                throw error(HttpStatus.CONFLICT, "Keep customers with orders so their transaction history remains available.");
            }
            userService.deleteById(customerId);
        }
        return map("success", true);
    }

    private static void validateNameAndEmail(String name, String email) {
        if (name.isBlank() || name.length() > 100 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw error(HttpStatus.BAD_REQUEST, "A name and a valid email are required.");
        }
    }

    private static Long guestCartId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute("websiteGuestCartId") instanceof Long guest ? guest : null;
    }

    private void migrateGuestCart(Long guest, HttpSession session) {
        if (guest == null || !(session.getAttribute("loggedInUserId") instanceof Long owner)) {
            return;
        }
        for (CartItem original : carts.findByUserId(guest)) {
            var copy = new CartItem(owner, original.getProductId(), original.getProductName(),
                    original.getQuantity(), original.getUnitPrice());
            copy.setSelectedSize(original.getSelectedSize());
            copy.setProductType(original.getProductType());
            copy.setImageUrl(original.getImageUrl());
            cart.addToCart(copy);
            carts.delete(original);
        }
    }
}
