package de.htwsaar.monitoring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Security configuration for the monitoring application.
 *
 * - Defines an in-memory admin user with ADMIN role.
 * - Credentials are read from environment variables:
 *   APP_ADMIN_USER, APP_ADMIN_PASSWORD.
 * - Protects write endpoints (/api/checks/run, /api/alerts) with role-based access control.
 */
@Configuration
public class SecurityConfig {

    /**
     * Provides the UserDetailsService with a single ADMIN user.
     * Username and password are configured via environment variables.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        String username = System.getenv("APP_ADMIN_USER");
        if (username == null || username.isBlank()) {
            username = "admin";
        }

        String password = System.getenv("APP_ADMIN_PASSWORD");
        if (password == null || password.isBlank()) {
            password = "change-me-in-production";
        }

        var admin = User.withDefaultPasswordEncoder()
                .username(username)
                .password(password)
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }

    /**
     * Configures HTTP security:
     * - Static assets and health endpoints are public.
     * - Read-only APIs for dashboard are public.
     * - Write APIs (run checks, receive alerts) require ADMIN role.
     * - All other requests require authentication.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public: static files and health endpoints
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/style.css",
                                "/app.js",
                                "/actuator/health",
                                "/actuator/info",
                                "/actuator/prometheus"
                        ).permitAll()

                        // Public: read-only APIs for dashboard
                        .requestMatchers(
                                "/api/devices",
                                "/api/checks/latest",
                                "/api/incidents/**"
                        ).permitAll()

                        // Admin only: trigger checks and receive alerts
                        .requestMatchers(
                                "/api/checks/run",
                                "/api/alerts"
                        ).hasRole("ADMIN")

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                .httpBasic(withDefaults());

        return http.build();
    }
}