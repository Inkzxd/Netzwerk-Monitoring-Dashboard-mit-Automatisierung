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
 */
@Configuration
public class SecurityConfig {

    /**
     * Defines the in-memory administrator account.
     *
     * <p>Credentials are read from APP_ADMIN_USER and APP_ADMIN_PASSWORD.
     * Fallback values are used only for local development.</p>
     *
     * @return user details service with one ADMIN user
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
     * Configures public read-only endpoints and protected write endpoints.
     *
     * @param http Spring Security HTTP configuration
     * @return configured filter chain
     * @throws Exception when configuration cannot be built
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/style.css",
                                "/app.js",
                                "/actuator/health",
                                "/actuator/info",
                                "/actuator/prometheus"
                        ).permitAll()

                        .requestMatchers(
                                "/api/devices",
                                "/api/checks/latest",
                                "/api/checks/history",
                                "/api/incidents/**"
                        ).permitAll()

                        .requestMatchers(
                                "/api/checks/run",
                                "/api/alerts"
                        ).hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .httpBasic(withDefaults());

        return http.build();
    }
}