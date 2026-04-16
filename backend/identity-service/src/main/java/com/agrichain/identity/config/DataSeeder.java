package com.agrichain.identity.config;

import com.agrichain.common.enums.UserRole;
import com.agrichain.identity.entity.User;
import com.agrichain.identity.entity.UserStatus;
import com.agrichain.identity.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        seedUser("admin_demo", "Admin@1234", "admin@farmconnect.com", UserRole.ADMINISTRATOR);
        seedUser("farmer_demo", "Farm@1234", "farmer@farmconnect.com", UserRole.FARMER);
        seedUser("trader_demo", "Trade@1234", "trader@farmconnect.com", UserRole.TRADER);
        seedUser("officer_demo", "Officer@1234", "officer@farmconnect.com", UserRole.MARKET_OFFICER);
        seedUser("compliance_demo", "Comp@1234", "compliance@farmconnect.com", UserRole.COMPLIANCE_OFFICER);
        seedUser("auditor_demo", "Audit@1234", "auditor@farmconnect.com", UserRole.GOVERNMENT_AUDITOR);
        seedUser("manager_demo", "Manager@1234", "manager@farmconnect.com", UserRole.PROGRAM_MANAGER);
    }

    private void seedUser(String username, String password, String email, UserRole role) {
        userRepository.findByUsername(username).ifPresentOrElse(
            user -> {
                if (user.getRole() != role) {
                    user.setRole(role);
                    userRepository.save(user);
                    System.out.println("Updated role for existing user: " + username + " to [" + role + "]");
                }
            },
            () -> {
                User user = new User();
                user.setUsername(username);
                user.setPasswordHash(passwordEncoder.encode(password));
                user.setEmail(email);
                user.setRole(role);
                user.setStatus(UserStatus.Active);
                userRepository.save(user);
                System.out.println("Seeded demo user: " + username + " [" + role + "]");
            }
        );
    }
}
