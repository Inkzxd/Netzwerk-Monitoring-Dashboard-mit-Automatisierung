package de.htwsaar.monitoring.controller;

import de.htwsaar.monitoring.config.SecurityConfig;
import de.htwsaar.monitoring.model.CheckResult;
import de.htwsaar.monitoring.model.Device;
import de.htwsaar.monitoring.service.DeviceCheckService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeviceController.class)
@Import(SecurityConfig.class)
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeviceCheckService deviceCheckService;

    @Test
    void shouldReturnDevices() throws Exception {
        when(deviceCheckService.getDevices()).thenReturn(List.of(
                new Device("router", "Router", "192.0.2.1", 443, true),
                new Device("switch", "Switch", "192.0.2.2", 80, true)
        ));

        mockMvc.perform(get("/api/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("router"))
                .andExpect(jsonPath("$[0].name").value("Router"))
                .andExpect(jsonPath("$[0].host").value("192.0.2.1"))
                .andExpect(jsonPath("$[0].port").value(443))
                .andExpect(jsonPath("$[1].id").value("switch"));

        verify(deviceCheckService).getDevices();
    }

    @Test
    void shouldReturnEmptyListWhenNoDevices() throws Exception {
        when(deviceCheckService.getDevices()).thenReturn(List.of());

        mockMvc.perform(get("/api/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturnDeviceByName() throws Exception {
        Device device = new Device("router", "Router", "192.0.2.1", 443, true);
        when(deviceCheckService.getDevices()).thenReturn(List.of(device));

        mockMvc.perform(get("/api/devices/router"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("router"))
                .andExpect(jsonPath("$.name").value("Router"));
    }

    @Test
    void shouldReturnNotFoundForUnknownDevice() throws Exception {
        when(deviceCheckService.getDevices()).thenReturn(List.of(
                new Device("router", "Router", "192.0.2.1", 443, true)
        ));

        mockMvc.perform(get("/api/devices/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnLatestChecks() throws Exception {
        CheckResult result = new CheckResult(
                "Router",
                true,
                15,
                LocalDateTime.now(),
                null
        );

        when(deviceCheckService.getLatestResults()).thenReturn(List.of(result));

        mockMvc.perform(get("/api/checks/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].deviceName").value("Router"))
                .andExpect(jsonPath("$[0].up").value(true))
                .andExpect(jsonPath("$[0].latencyMs").value(15));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRunChecksOnPost() throws Exception {
        CheckResult result = new CheckResult(
                "Router",
                true,
                10,
                LocalDateTime.now(),
                null
        );

        when(deviceCheckService.checkAllDevices()).thenReturn(List.of(result));

        mockMvc.perform(post("/api/checks/run")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].deviceName").value("Router"));

        verify(deviceCheckService).checkAllDevices();
    }
}