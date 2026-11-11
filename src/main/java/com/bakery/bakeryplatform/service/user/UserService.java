package com.bakery.bakeryplatform.service.user;

import com.bakery.bakeryplatform.model.user.AdminUser;
import com.bakery.bakeryplatform.model.user.Customer;
import com.bakery.bakeryplatform.model.user.User;
import com.bakery.bakeryplatform.repository.user.AdminUserRepository;
import com.bakery.bakeryplatform.repository.user.DeliveryRepository;
import com.bakery.bakeryplatform.repository.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class UserService {
    private static final int PASSWORD_ITERATIONS = 210_000;
    private static final int PASSWORD_KEY_LENGTH = 256;
    private static final int PASSWORD_SALT_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;
    private final DeliveryRepository deliveryRepository;

    public UserService(UserRepository userRepository, AdminUserRepository adminUserRepository,
                       DeliveryRepository deliveryRepository) {
        this.userRepository = userRepository;
        this.adminUserRepository = adminUserRepository;
        this.deliveryRepository = deliveryRepository;
    }

    /** Checks the admin_user table (username or email + password). */
    public Optional<AdminUser> authenticateAdmin(String usernameOrEmail, String password) {
        String identity = normalizeIdentity(usernameOrEmail);
        AdminUser admin = adminUserRepository.findByUsername(identity);
        if (admin == null) {
            admin = adminUserRepository.findByEmail(identity);
        }
        if (admin != null && passwordMatches(password, admin.getPassword())) {
            upgradeAdminPassword(admin, password);
            return Optional.of(admin);
        }
        return Optional.empty();
    }

    public AdminUser createAdmin(String username, String email, String password) {
        AdminUser admin = new AdminUser(
                normalizeIdentity(username),
                normalizeIdentity(email),
                encodePassword(password));
        return adminUserRepository.save(admin);
    }

    public void ensureAdmin(String username, String email, String password) {
        if (adminUserRepository.findByUsername(normalizeIdentity(username)) == null) {
            createAdmin(username, email, password);
        }
    }

    public AdminUser findAdminById(Long id) {
        return adminUserRepository.findById(id).orElse(null);
    }

    public List<Customer> findAllCustomers() {
        return userRepository.findAll().stream()
                .filter(u -> u instanceof Customer)
                .map(u -> (Customer) u)
                .toList();
    }

    public Customer register(Customer customer) {
        customer.setEmail(normalizeIdentity(customer.getEmail()));
        customer.setPassword(encodePassword(customer.getPassword()));
        return userRepository.save(customer);
    }

    /** Looks the user up by username first, then by email, and checks the password. */
    public Optional<User> authenticate(String usernameOrEmail, String password) {
        String identity = normalizeIdentity(usernameOrEmail);
        User user = userRepository.findByUsername(identity);
        if (user == null) {
            user = userRepository.findByEmail(identity);
        }
        if (user != null && passwordMatches(password, user.getPassword())) {
            upgradeUserPassword(user, password);
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    /** Updates only the editable fields (name, phone, address, password); email stays as it is. */
    public void updateProfile(Long userId, Customer form) {
        User existing = userRepository.findById(userId).orElse(null);
        if (existing instanceof Customer customer) {
            if (form.getUsername() != null && !form.getUsername().isBlank()) {
                customer.setUsername(form.getUsername().trim());
            }
            if (form.getPhoneNumber() != null) {
                customer.setPhoneNumber(form.getPhoneNumber().trim());
            }
            if (form.getAddress() != null) {
                customer.setAddress(form.getAddress().trim());
            }
            if (form.getPassword() != null && !form.getPassword().isBlank()) {
                customer.setPassword(encodePassword(form.getPassword()));
            }
            userRepository.save(customer);
        }
    }

    @Transactional
    public void deleteById(Long id) {
        deliveryRepository.deleteByCustomerId(id); // user ge deliveries issella ain karanawa
        userRepository.deleteById(id);
    }

    public boolean customerEmailExists(String email) {
        return userRepository.findByEmail(normalizeIdentity(email)) != null;
    }

    public String encodePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        byte[] salt = new byte[PASSWORD_SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = derivePassword(password.toCharArray(), salt, PASSWORD_ITERATIONS);
        return "pbkdf2$" + PASSWORD_ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (!storedPassword.startsWith("pbkdf2$")) {
            return MessageDigest.isEqual(
                    rawPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    storedPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        try {
            String[] parts = storedPassword.split("\\$", -1);
            if (parts.length != 4) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            if (iterations < 1 || salt.length == 0 || expected.length == 0) {
                return false;
            }
            return MessageDigest.isEqual(expected,
                    derivePassword(rawPassword.toCharArray(), salt, iterations));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private void upgradeAdminPassword(AdminUser admin, String rawPassword) {
        if (!admin.getPassword().startsWith("pbkdf2$")) {
            admin.setPassword(encodePassword(rawPassword));
            adminUserRepository.save(admin);
        }
    }

    private void upgradeUserPassword(User user, String rawPassword) {
        if (!user.getPassword().startsWith("pbkdf2$")) {
            user.setPassword(encodePassword(rawPassword));
            userRepository.save(user);
        }
    }

    private static byte[] derivePassword(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, PASSWORD_KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Password hashing is unavailable.", exception);
        } finally {
            spec.clearPassword();
            java.util.Arrays.fill(password, '\0');
        }
    }

    private static String normalizeIdentity(String identity) {
        return identity == null ? null : identity.trim().toLowerCase(Locale.ROOT);
    }
}
