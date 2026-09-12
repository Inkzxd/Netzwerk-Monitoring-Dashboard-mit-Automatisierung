package de.htwsaar.monitoring.controller;

import de.htwsaar.monitoring.config.SecurityConfig;
import de.htwsaar.monitoring.model.CheckResultEntity;
import de.htwsaar.monitoring.model.CheckResultRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CheckHistoryController.class)
@Import(SecurityConfig.class)
class CheckHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CheckResultRepository repository;

    @Test
    void shouldReturnAllHistoryWhenNoParameters() throws Exception {
        CheckResultEntity result = new CheckResultEntity(
                "Router",
                "192.0.2.1",
                true,
                10,
                LocalDateTime.now(),
                null
        );

        when(repository.findAllByOrderByCheckedAtDesc()).thenReturn(List.of(result));

        mockMvc.perform(get("/api/checks/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].deviceName").value("Router"));

        verify(repository).findAllByOrderByCheckedAtDesc();
    }

    @Test
    void shouldRejectNonPositiveLimit() throws Exception {
        mockMvc.perform(get("/api/checks/history?limit=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_parameter"));

        verifyNoInteractions(repository);
    }

    @Test
    void shouldRejectNegativeLimit() throws Exception {
        mockMvc.perform(get("/api/checks/history?limit=-5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_parameter"));

        verifyNoInteractions(repository);
    }

    @Test
    void shouldRejectDeviceAndSinceTogether() throws Exception {
        mockMvc.perform(get("/api/checks/history?device=router&since=2026-09-01T10:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_parameter"));

        verifyNoInteractions(repository);
    }

    @Test
    void shouldFilterByDevice() throws Exception {
        CheckResultEntity result = new CheckResultEntity(
                "Router",
                "192.0.2.1",
                true,
                10,
                LocalDateTime.now(),
                null
        );

        when(repository.findByDeviceNameOrderByCheckedAtDesc("router"))
                .thenReturn(List.of(result));

        mockMvc.perform(get("/api/checks/history?device=router"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].deviceName").value("Router"));

        verify(repository).findByDeviceNameOrderByCheckedAtDesc("router");
    }

    @Test
    void shouldFilterBySince() throws Exception {
        LocalDateTime since = LocalDateTime.of(2026, 9, 1, 10, 0, 0);
        CheckResultEntity result = new CheckResultEntity(
                "Router",
                "192.0.2.1",
                true,
                10,
                since.plusMinutes(5),
                null
        );

        when(repository.findByCheckedAtAfterOrderByCheckedAtDesc(since))
                .thenReturn(List.of(result));

        mockMvc.perform(get("/api/checks/history?since=2026-09-01T10:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].deviceName").value("Router"));

        verify(repository).findByCheckedAtAfterOrderByCheckedAtDesc(since);
    }

    @Test
    void shouldApplyLimit() throws Exception {
        CheckResultEntity result1 = new CheckResultEntity(
                "Router",
                "192.0.2.1",
                true,
                10,
                LocalDateTime.now(),
                null
        );
        CheckResultEntity result2 = new CheckResultEntity(
                "Switch",
                "192.0.2.2",
                true,
                12,
                LocalDateTime.now().minusMinutes(5),
                null
        );

        when(repository.findAllByOrderByCheckedAtDesc())
                .thenReturn(List.of(result1, result2));

        mockMvc.perform(get("/api/checks/history?limit=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].deviceName").value("Router"));
    }

    @Test
    void shouldReturnEmptyListWhenNoResults() throws Exception {
        when(repository.findAllByOrderByCheckedAtDesc()).thenReturn(List.of());

        mockMvc.perform(get("/api/checks/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}