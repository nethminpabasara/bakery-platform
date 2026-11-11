package com.bakery.bakeryplatform.config;

import com.bakery.bakeryplatform.service.user.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedAdmin(
            UserService userService,
            @Value("${app.admin.username:}") String username,
            @Value("${app.admin.email:}") String email,
            @Value("${app.admin.password:}") String password) {
        return args -> {
            if (StringUtils.hasText(username) && StringUtils.hasText(email) && StringUtils.hasText(password)) {
                userService.ensureAdmin(username, email, password);
            }
        };
    }
}
