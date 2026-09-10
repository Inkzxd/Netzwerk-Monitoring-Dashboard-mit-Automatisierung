package de.htwsaar.monitoring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder
    ) {
        String username = System.getenv("APP_ADMIN_USER");
        if (username == null || username.isBlank()) {
            username = "admin";
        }

        String password = System.getenv("APP_ADMIN_PASSWORD");
        if (password == null || password.isBlank()) {
            password = "change-me-in-production";
        }

        var admin = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }

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
                                "/actuator/prometheus",
                                "/api/status"
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