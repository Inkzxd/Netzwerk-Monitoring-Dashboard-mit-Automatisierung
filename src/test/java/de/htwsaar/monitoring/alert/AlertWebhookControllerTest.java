package de.htwsaar.monitoring.alert;

import de.htwsaar.monitoring.config.SecurityConfig;
import de.htwsaar.monitoring.incident.IncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlertWebhookController.class)
@Import(SecurityConfig.class)
class AlertWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IncidentService incidentService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAcceptValidWebhookWithEmptyAlerts() throws Exception {
        String json = """
                {
                  "version": "4",
                  "groupKey": "{}:{alertname=NetworkDeviceDown}",
                  "status": "firing",
                  "receiver": "spring-boot-webhook",
                  "groupLabels": {},
                  "commonLabels": {},
                  "commonAnnotations": {},
                  "alerts": []
                }
                """;

        mockMvc.perform(post("/api/alerts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verifyNoInteractions(incidentService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAcceptValidWebhookWithAlerts() throws Exception {
        String json = """
                {
                  "version": "4",
                  "groupKey": "{}:{alertname=NetworkDeviceDown}",
                  "status": "firing",
                  "receiver": "spring-boot-webhook",
                  "groupLabels": {},
                  "commonLabels": {},
                  "commonAnnotations": {},
                  "alerts": [
                    {
                      "status": "firing",
                      "labels": {
                        "alertname": "NetworkDeviceDown",
                        "device": "Router",
                        "host": "192.0.2.1",
                        "severity": "critical"
                      },
                      "annotations": {
                        "summary": "Device is down",
                        "description": "Router unreachable"
                      },
                      "startsAt": "2026-09-11T10:00:00Z",
                      "endsAt": null,
                      "generatorURL": "http://prometheus:9090/graph",
                      "fingerprint": "abc123"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/alerts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(incidentService, times(1)).process(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRejectMalformedJson() throws Exception {
        mockMvc.perform(post("/api/alerts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_json"));

        verifyNoInteractions(incidentService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRejectNullPayload() throws Exception {
        mockMvc.perform(post("/api/alerts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(incidentService);
    }

    @Test
    void shouldRequireAuthentication() throws Exception {
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

        verifyNoInteractions(incidentService);
    }

    @Test
    void shouldRejectNonAdminUser() throws Exception {
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

        verifyNoInteractions(incidentService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAcceptResolvedAlert() throws Exception {
        String json = """
                {
                  "version": "4",
                  "groupKey": "{}:{alertname=NetworkDeviceDown}",
                  "status": "resolved",
                  "receiver": "spring-boot-webhook",
                  "groupLabels": {},
                  "commonLabels": {},
                  "commonAnnotations": {},
                  "alerts": [
                    {
                      "status": "resolved",
                      "labels": {
                        "alertname": "NetworkDeviceDown",
                        "device": "Router",
                        "host": "192.0.2.1",
                        "severity": "critical"
                      },
                      "annotations": {
                        "summary": "Device recovered",
                        "description": "Router is back online"
                      },
                      "startsAt": "2026-09-11T10:00:00Z",
                      "endsAt": "2026-09-11T10:10:00Z",
                      "generatorURL": "http://prometheus:9090/graph",
                      "fingerprint": "abc123"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/alerts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(incidentService, times(1)).process(any());
    }
}