package de.htwsaar.monitoring.service;

import de.htwsaar.monitoring.config.MonitoringProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PingCheckServiceTest {

    @Test
    void shouldSkipDisabledDevices() {
        MonitoringProperties properties = propertiesWith(
                new MonitoringProperties.DeviceProperties(
                        "disabled",
                        "Disabled",
                        "localhost",
                        1,
                        false
                )
        );

        PingCheckService service = service(properties);

        assertTrue(service.checkAllDevices().isEmpty());
        assertTrue(service.getLatestResults().isEmpty());
    }

    @Test
    void shouldReturnImmutableLatestResults() {
        MonitoringProperties properties = propertiesWith(
                new MonitoringProperties.DeviceProperties(
                        "loopback",
                        "Loopback",
                        "127.0.0.1",
                        1,
                        true
                )
        );

        PingCheckService service = service(properties);
        service.checkAllDevices();

        assertThrows(
                UnsupportedOperationException.class,
                () -> service.getLatestResults().add(
                        new PingCheckService.PingResult(
                                "x",
                                "x",
                                true,
                                1,
                                null
                        )
                )
        );
    }

    @Test
    void shouldRegisterPingMetricsForConfiguredDevice() {
        MonitoringProperties properties = propertiesWith(
                new MonitoringProperties.DeviceProperties(
                        "loopback",
                        "Loopback",
                        "127.0.0.1",
                        1,
                        true
                )
        );
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        new PingCheckService(properties, registry);

        assertNotNull(registry.find("network_device_ping_up")
                .tag("device_id", "loopback")
                .gauge());
        assertNotNull(registry.find("network_device_ping_latency_ms")
                .tag("device_id", "loopback")
                .gauge());
    }

    @Test
    void shouldReturnResultForReachableOrUnreachableHostWithoutThrowing() {
        MonitoringProperties properties = propertiesWith(
                new MonitoringProperties.DeviceProperties(
                        "loopback",
                        "Loopback",
                        "127.0.0.1",
                        1,
                        true
                )
        );

        PingCheckService service = service(properties);

        PingCheckService.PingResult result = assertDoesNotThrow(
                () -> service.checkAllDevices().get(0)
        );

        assertEquals("Loopback", result.deviceName());
        assertEquals("127.0.0.1", result.host());
        assertTrue(result.latencyMs() >= 0);
    }

    private static PingCheckService service(MonitoringProperties properties) {
        return new PingCheckService(properties, new SimpleMeterRegistry());
    }

    private static MonitoringProperties propertiesWith(
            MonitoringProperties.DeviceProperties... devices
    ) {
        return new MonitoringProperties(
                Duration.ofMillis(100),
                Duration.ofDays(30),
                List.of(devices)
        );
    }
}
