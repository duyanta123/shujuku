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
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
