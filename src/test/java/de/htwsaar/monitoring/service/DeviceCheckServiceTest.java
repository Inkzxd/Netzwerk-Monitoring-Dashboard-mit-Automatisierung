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
                                        "test-device",   // id
                                        "Test Device",   // name
                                        "localhost",     // host
                                        8080,           // port
                                        true            // enabled
                                )
                        )
                );

        DeviceCheckService service =
                new DeviceCheckService(
                        properties,
                        new SimpleMeterRegistry(),
                        null // CheckResultRepository not used in this test
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
                        new SimpleMeterRegistry(),
                        null // CheckResultRepository not used in this test
                );

        Device device =
                new Device(
                        "invalid-device", // id
                        "Invalid Device",  // name
                        "192.0.2.1",       // host
                        65500,            // port
                        true              // enabled
                );

        CheckResult result = service.checkDevice(device);

        assertFalse(result.isUp());
        assertTrue(result.getLatencyMs() >= 0);
    }
}