package de.htwsaar.monitoring.service;

import de.htwsaar.monitoring.config.MonitoringProperties;
import de.htwsaar.monitoring.model.CheckResult;
import de.htwsaar.monitoring.model.Device;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeviceCheckServiceTest {

    @Test
    void shouldReturnDeviceListFromConfiguration() {
        MonitoringProperties properties =
                new MonitoringProperties(
                        Duration.ofMillis(100),
                        List.of(
                                new MonitoringProperties.DeviceProperties(
                                        "Test Device",
                                        "localhost",
                                        8080,
                                        true
                                )
                        )
                );

        DeviceCheckService service =
                new DeviceCheckService(
                        properties,
                        new SimpleMeterRegistry()
                );

        assertEquals(1, service.getDevices().size());
        assertEquals(
                "Test Device",
                service.getDevices().get(0).getName()
        );
    }

    @Test
    void shouldReturnUnreachableDeviceAsDown() {
        MonitoringProperties properties =
                new MonitoringProperties(
                        Duration.ofMillis(100),
                        List.of()
                );

        DeviceCheckService service =
                new DeviceCheckService(
                        properties,
                        new SimpleMeterRegistry()
                );

        Device device =
                new Device(
                        "Invalid Device",
                        "192.0.2.1",
                        65500,
                        true
                );

        CheckResult result = service.checkDevice(device);

        assertFalse(result.isUp());
        assertTrue(result.getLatencyMs() >= 0);
    }
}