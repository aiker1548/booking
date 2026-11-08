package com.hotelbooking.booking.config;

import com.hotelbooking.booking.entity.User;
import com.hotelbooking.booking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class DemoUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.demo-data.enabled:true}")
    private boolean enabled;

    public DemoUserInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        createUser("demo-user", "DemoUser123!", User.Role.USER);
        createUser("demo-admin", "DemoAdmin123!", User.Role.ADMIN);
    }

    private void createUser(String username, String password, User.Role role) {
        if (userRepository.existsByUsername(username)) {
            return;
        }

        User user = new User(
                UUID.randomUUID(),
                username,
                passwordEncoder.encode(password),
                role
        );

        userRepository.save(user);
    }
}
