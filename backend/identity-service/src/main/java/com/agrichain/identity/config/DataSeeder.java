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
        seedUser("admin_demo", "Admin@1234", "admin@farmconnect.com", UserRole.Administrator);
        seedUser("farmer_demo", "Farm@1234", "farmer@farmconnect.com", UserRole.Farmer);
        seedUser("trader_demo", "Trade@1234", "trader@farmconnect.com", UserRole.Trader);
        seedUser("officer_demo", "Officer@1234", "officer@farmconnect.com", UserRole.Market_Officer);
        seedUser("compliance_demo", "Comp@1234", "compliance@farmconnect.com", UserRole.Compliance_Officer);
        seedUser("auditor_demo", "Audit@1234", "auditor@farmconnect.com", UserRole.Government_Auditor);
        seedUser("manager_demo", "Manager@1234", "manager@farmconnect.com", UserRole.Program_Manager);
    }

    private void seedUser(String username, String password, String email, UserRole role) {
        if (!userRepository.existsByUsername(username)) {
            User user = new User();
            user.setUsername(username);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setEmail(email);
            user.setRole(role);
            user.setStatus(UserStatus.Active);
            userRepository.save(user);
            System.out.println("Seeded demo user: " + username + " [" + role + "]");
        }
    }
}
