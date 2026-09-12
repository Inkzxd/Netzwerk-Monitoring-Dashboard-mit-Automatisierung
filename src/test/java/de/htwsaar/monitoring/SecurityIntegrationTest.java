package de.htwsaar.monitoring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:sqlite:target/test-monitoring.db",
                "spring.datasource.driver-class-name=org.sqlite.JDBC",
                "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "monitoring.devices="
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowPublicStatusEndpoint() throws Exception {
        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void shouldAllowPublicHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowPublicStaticResources() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/style.css"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowPublicDeviceList() throws Exception {
        mockMvc.perform(get("/api/devices"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowPublicLatestChecks() throws Exception {
        mockMvc.perform(get("/api/checks/latest"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowPublicCheckHistory() throws Exception {
        mockMvc.perform(get("/api/checks/history"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowPublicIncidents() throws Exception {
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/incidents/active"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRequireAuthForManualChecks() throws Exception {
        mockMvc.perform(post("/api/checks/run"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRequireAuthForAlertWebhook() throws Exception {
        String json = """
                {
                  "version": "4",
                  "groupKey": "{}",
                  "status": "firing",
                  "receiver": "test",
                  "groupLabels": {},
                  "commonLabels": {},
                  "commonAnnotations": {},
                  "alerts": []
                }
                """;

        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAdminUserForManualChecks() throws Exception {
        String username = System.getenv("APP_ADMIN_USER");
        if (username == null || username.isBlank()) {
            username = "admin";
        }

        String password = System.getenv("APP_ADMIN_PASSWORD");
        if (password == null || password.isBlank()) {
            password = "change-me-in-production";
        }

        mockMvc.perform(post("/api/checks/run")
                        .with(httpBasic(username, password)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAdminUserForAlertWebhook() throws Exception {
        String username = System.getenv("APP_ADMIN_USER");
        if (username == null || username.isBlank()) {
            username = "admin";
        }

        String password = System.getenv("APP_ADMIN_PASSWORD");
        if (password == null || password.isBlank()) {
            password = "change-me-in-production";
        }

        String json = """
                {
                  "version": "4",
                  "groupKey": "{}",
                  "status": "firing",
                  "receiver": "test",
                  "groupLabels": {},
                  "commonLabels": {},
                  "commonAnnotations": {},
                  "alerts": []
                }
                """;

        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(httpBasic(username, password)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectNonAdminUserForManualChecks() throws Exception {
        mockMvc.perform(post("/api/checks/run")
                        .with(httpBasic("user", "password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectNonAdminUserForAlertWebhook() throws Exception {
        String json = """
                {
                  "version": "4",
                  "groupKey": "{}",
                  "status": "firing",
                  "receiver": "test",
                  "groupLabels": {},
                  "commonLabels": {},
                  "commonAnnotations": {},
                  "alerts": []
                }
                """;

        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(httpBasic("user", "password")))
                .andExpect(status().isUnauthorized());
    }
}