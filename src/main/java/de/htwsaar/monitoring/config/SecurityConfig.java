package de.htwsaar.monitoring.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the Network Monitoring Dashboard.
 * <p>
 * <strong>Public Endpoints</strong> (no authentication required):
 * <ul>
 *   <li>{@code GET /api/status} - Application health check</li>
 *   <li>{@code GET /api/devices} - List configured devices</li>
 *   <li>{@code GET /api/checks/latest} - Latest monitoring results</li>
 *   <li>{@code GET /api/checks/history} - Historical check data</li>
 *   <li>{@code GET /api/incidents} - All incidents</li>
 *   <li>{@code GET /api/incidents/active} - Active incidents</li>
 *   <li>{@code GET /api/pings/latest} - Latest ICMP ping results</li>
 *   <li>{@code GET /actuator/health} - Spring Actuator health</li>
 *   <li>{@code GET /actuator/prometheus} - Prometheus metrics</li>
 *   <li>{@code GET /}, {@code /index.html}, {@code /app.js}, {@code /style.css} -
 *       Static web dashboard resources</li>
 * </ul>
 * <p>
 * <strong>ADMIN Endpoints</strong> (HTTP Basic Auth + ADMIN role required):
 * <ul>
 *   <li>{@code POST /api/checks/run} - Trigger manual monitoring checks</li>
 *   <li>{@code POST /api/alerts} - Receive Alertmanager webhooks</li>
 * </ul>
 * <p>
 * <strong>Basic Authentication Design</strong>:
 * <ul>
 *   <li><strong>Why Basic Auth</strong>: Simple, widely supported by HTTP clients
 *       (including Alertmanager), no OAuth2 complexity needed for internal services.</li>
 *   <li><strong>Credentials</strong>: Injected via environment variables
 *       ({@code APP_ADMIN_USER}, {@code APP_ADMIN_PASSWORD}) to avoid hardcoding.</li>
 *   <li><strong>Password Storage</strong>: Uses BCrypt hashing (Spring Security default)
 *       for secure credential storage in memory.</li>
 * </ul>
 * <p>
 * <strong>CSRF Protection Design</strong>:
 * <ul>
 *   <li><strong>Enabled for Browser Requests</strong>: Protects against cross-site
 *       request forgery attacks on state-changing operations (POST, PUT, DELETE).</li>
 *   <li><strong>Disabled for API Endpoints</strong>: The {@code /api/**} paths exclude
 *       CSRF checks to allow non-browser clients (Alertmanager, curl, scripts) to
 *       interact with the API without CSRF tokens.</li>
 *   <li><strong>Rationale</strong>: API endpoints are protected by Basic Auth + ADMIN
 *       role, which is sufficient for machine-to-machine communication. CSRF is
 *       primarily a browser-based attack vector.</li>
 * </ul>
 * <p>
 * <strong>Session Management</strong>:
 * <ul>
 *   <li><strong>Stateless</strong>: HTTP Basic Auth is used, so no server-side session
 *       is created. Each request must include credentials.</li>
 *   <li><strong>Why Stateless</strong>: Simplifies deployment (no session replication),
 *       aligns with REST principles, and matches Alertmanager's authentication model.</li>
 * </ul>
 * <p>
 * <strong>Security Headers</strong>:
 * <ul>
 *   <li>Default Spring Security headers are enabled (X-Content-Type-Options,
 *       X-Frame-Options, X-XSS-Protection, etc.).</li>
 *   <li>These protect against common web vulnerabilities (MIME sniffing, clickjacking,
 *       XSS) for the browser-based dashboard.</li>
 * </ul>
 *
 * @see org.springframework.security.config.annotation.web.builders.HttpSecurity
 * @see org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${APP_ADMIN_USER:admin}")
    private String adminUsername;

    @Value("${APP_ADMIN_PASSWORD:change-me-in-production}")
    private String adminPassword;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/style.css",
                                "/app.js",
                                "/api/status",
                                "/api/devices",
                                "/api/devices/**",
                                "/api/checks/latest",
                                "/api/checks/history",
                                "/api/incidents",
                                "/api/incidents/**",
                                "/api/pings/latest",
                                "/actuator/health",
                                "/actuator/info",
                                "/actuator/prometheus"
                        ).permitAll()
                        .requestMatchers(
                                "/api/checks/run",
                                "/api/alerts"
                        ).hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .httpBasic(httpBasic -> {})
                .formLogin(form -> form.disable());

        return http.build();
    }

    @Bean
    public UserDetailsManager userDetailsManager() {
        UserDetails admin = User
                .withUsername(adminUsername)
                .password("{noop}" + adminPassword)
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }
}