package de.htwsaar.monitoring.service;

import de.htwsaar.monitoring.config.MonitoringProperties;
import de.htwsaar.monitoring.model.CheckResult;
import de.htwsaar.monitoring.model.CheckResultEntity;
import de.htwsaar.monitoring.model.CheckResultRepository;
import de.htwsaar.monitoring.model.Device;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceCheckServiceTest {

    @Mock
    private CheckResultRepository repository;

    @Test
    void shouldReturnDeviceListFromConfiguration() {
        MonitoringProperties properties = new MonitoringProperties(
                Duration.ofMillis(100),
                Duration.ofDays(30),
                List.of(
                        new MonitoringProperties.DeviceProperties(
                                "test-device",
                                "Test Device",
                                "localhost",
                                8080,
                                true
                        )
                )
        );

        DeviceCheckService service = new DeviceCheckService(
                properties,
                new SimpleMeterRegistry(),
                null
        );

        assertEquals(1, service.getDevices().size());
        assertEquals("Test Device", service.getDevices().get(0).getName());
    }

    @Test
    void shouldSkipDisabledDevices() {
        MonitoringProperties properties = new MonitoringProperties(
                Duration.ofMillis(100),
                Duration.ofDays(30),
                List.of(
                        new MonitoringProperties.DeviceProperties(
                                "disabled",
                                "Disabled Device",
                                "localhost",
                                1,
                                false
                        )
                )
        );

        DeviceCheckService service = new DeviceCheckService(
                properties,
                new SimpleMeterRegistry(),
                repository
        );

        List<CheckResult> results = service.checkAllDevices();

        assertTrue(results.isEmpty());
        verify(repository, never()).save(any(CheckResultEntity.class));
    }

    @Test
    void shouldReturnDownWhenTcpConnectionFails() {
        MonitoringProperties properties = new MonitoringProperties(
                Duration.ofMillis(100),
                Duration.ofDays(30),
                List.of(
                        new MonitoringProperties.DeviceProperties(
                                "invalid",
                                "Invalid Device",
                                "192.0.2.1",
                                65000,
                                true
                        )
                )
        );

        DeviceCheckService service = new DeviceCheckService(
                properties,
                new SimpleMeterRegistry(),
                repository
        );

        Device device = service.getDevices().get(0);
        CheckResult result = service.checkDevice(device);

        assertFalse(result.isUp());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getLatencyMs() >= 0);
        assertEquals("Invalid Device", result.getDeviceName());
    }

    @Test
    void shouldPersistResultForEnabledDevice() {
        MonitoringProperties properties = new MonitoringProperties(
                Duration.ofMillis(100),
                Duration.ofDays(30),
                List.of(
                        new MonitoringProperties.DeviceProperties(
                                "test",
                                "Test Device",
                                "127.0.0.1",
                                1,
                                true
                        )
                )
        );

        DeviceCheckService service = new DeviceCheckService(
                properties,
                new SimpleMeterRegistry(),
                repository
        );

        List<CheckResult> results = service.checkAllDevices();

        verify(repository).save(any(CheckResultEntity.class));
        assertEquals(1, results.size());
        assertEquals(1, service.getLatestResults().size());
    }

    @Test
    void shouldPersistEachEnabledDeviceOnce() {
        MonitoringProperties properties = new MonitoringProperties(
                Duration.ofMillis(100),
                Duration.ofDays(30),
                List.of(
                        new MonitoringProperties.DeviceProperties(
                                "device1",
                                "Device 1",
                                "127.0.0.1",
                                1,
                                true
                        ),
                        new MonitoringProperties.DeviceProperties(
                                "device2",
                                "Device 2",
                                "127.0.0.1",
                                2,
                                true
                        ),
                        new MonitoringProperties.DeviceProperties(
                                "disabled",
                                "Disabled",
                                "127.0.0.1",
                                3,
                                false
                        )
                )
        );

        DeviceCheckService service = new DeviceCheckService(
                properties,
                new SimpleMeterRegistry(),
                repository
        );

        service.checkAllDevices();

        ArgumentCaptor<CheckResultEntity> captor = ArgumentCaptor.forClass(CheckResultEntity.class);
        verify(repository, times(2)).save(captor.capture());

        List<CheckResultEntity> saved = captor.getAllValues();
        assertEquals(2, saved.size());
        assertTrue(saved.stream().anyMatch(e -> e.getDeviceName().equals("Device 1")));
        assertTrue(saved.stream().anyMatch(e -> e.getDeviceName().equals("Device 2")));
    }

    @Test
    void shouldDeleteOldResults() {
        MonitoringProperties properties = new MonitoringProperties(
                Duration.ofMillis(100),
                Duration.ofDays(30),
                List.of()
        );

        CheckResultEntity oldResult = new CheckResultEntity(
                "Old Device",
                "localhost",
                false,
                10,
                LocalDateTime.now().minusDays(40),
                "timeout"
        );

        when(repository.findByCheckedAtBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(oldResult));

        DeviceCheckService service = new DeviceCheckService(
                properties,
                new SimpleMeterRegistry(),
                repository
        );

        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        service.deleteOldCheckResults(cutoff);

        verify(repository).findByCheckedAtBefore(cutoff);
        verify(repository).deleteAll(List.of(oldResult));
    }

    @Test
    void shouldCleanupHistoryUsingConfiguredRetention() {
        MonitoringProperties properties = new MonitoringProperties(
                Duration.ofMillis(100),
                Duration.ofDays(30),
                List.of()
        );

        CheckResultEntity oldResult = new CheckResultEntity(
                "Old Device",
                "localhost",
                false,
                10,
                LocalDateTime.now().minusDays(35),
                "timeout"
        );

        when(repository.findByCheckedAtBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(oldResult));

        DeviceCheckService service = new DeviceCheckService(
                properties,
                new SimpleMeterRegistry(),
                repository
        );

        service.cleanupHistory();

        verify(repository).findByCheckedAtBefore(any(LocalDateTime.class));
        verify(repository).deleteAll(List.of(oldResult));
    }

    @Test
    void shouldReturnEmptyListWhenNoDevicesConfigured() {
        MonitoringProperties properties = new MonitoringProperties(
                Duration.ofMillis(100),
                Duration.ofDays(30),
                List.of()
        );

        DeviceCheckService service = new DeviceCheckService(
                properties,
                new SimpleMeterRegistry(),
                repository
        );

        List<CheckResult> results = service.checkAllDevices();

        assertTrue(results.isEmpty());
        verify(repository, never()).save(any());
    }

    @Test
    void shouldReturnImmutableDeviceList() {
        MonitoringProperties properties = new MonitoringProperties(
                Duration.ofMillis(100),
                Duration.ofDays(30),
                List.of(
                        new MonitoringProperties.DeviceProperties(
                                "dev",
                                "Device",
                                "localhost",
                                80,
                                true
                        )
                )
        );

        DeviceCheckService service = new DeviceCheckService(
                properties,
                new SimpleMeterRegistry(),
                null
        );

        List<Device> devices = service.getDevices();

        assertThrows(UnsupportedOperationException.class, () -> devices.add(
                new Device("x", "x", "x", 1, true)
        ));
    }

    @Test
    void shouldReturnImmutableLatestResults() {
        MonitoringProperties properties = new MonitoringProperties(
                Duration.ofMillis(100),
                Duration.ofDays(30),
                List.of(
                        new MonitoringProperties.DeviceProperties(
                                "dev",
                                "Device",
                                "127.0.0.1",
                                1,
                                true
                        )
                )
        );

        DeviceCheckService service = new DeviceCheckService(
                properties,
                new SimpleMeterRegistry(),
                repository
        );

        service.checkAllDevices();

        List<CheckResult> results = service.getLatestResults();

        assertThrows(UnsupportedOperationException.class, () -> results.add(
                new CheckResult("x", true, 1, LocalDateTime.now(), null)
        ));
    }
}