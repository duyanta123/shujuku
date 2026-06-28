package com.labcourse.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProductionConfigValidator implements ApplicationRunner {

    private final Environment environment;

    public ProductionConfigValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!environment.matchesProfiles("prod")) {
            return;
        }

        String username = environment.getProperty("spring.datasource.username");
        String password = environment.getProperty("spring.datasource.password");

        if (isBlank(username) || isBlank(password)) {
            throw new IllegalStateException("Production database credentials must be configured explicitly.");
        }
        if ("root".equalsIgnoreCase(username.trim())) {
            throw new IllegalStateException("Production database user must not be root.");
        }
        if ("123456".equals(password) || "demo".equalsIgnoreCase(password)) {
            throw new IllegalStateException("Production database password must not use demo credentials.");
        }

        String jwtSecret = environment.getProperty("jwt.secret");
        if (isBlank(jwtSecret)) {
            throw new IllegalStateException("Production JWT secret must be configured explicitly.");
        }
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("Production JWT secret must be at least 32 characters long for security.");
        }
        if (jwtSecret.contains("dev") || jwtSecret.contains("change-in-production") || jwtSecret.contains("unsafe")) {
            throw new IllegalStateException("Production JWT secret must not use default/dev value.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
