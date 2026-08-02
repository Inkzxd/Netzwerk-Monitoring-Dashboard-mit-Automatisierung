package de.htwsaar.monitoring.service;

import de.htwsaar.monitoring.model.CheckResult;
import de.htwsaar.monitoring.model.Device;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DeviceCheckService {

    private final List<Device> devices = new ArrayList<>();
    private final Map<String, Double> deviceStatusMetrics = new ConcurrentHashMap<>();
    private final Map<String, Double> latencyMetrics = new ConcurrentHashMap<>();
    private final List<CheckResult> latestResults = new ArrayList<>();

    public DeviceCheckService(MeterRegistry registry) {
        devices.add(new Device("Local App", "app", 8080, true));
        devices.add(new Device("Grafana", "grafana", 3000, true));
        devices.add(new Device("Prometheus", "prometheus", 9090, true));

        for (Device device : devices) {
            deviceStatusMetrics.put(device.getName(), 0.0);
            latencyMetrics.put(device.getName(), 0.0);

            Gauge.builder("network_device_up", deviceStatusMetrics, m -> m.get(device.getName()))
                    .tag("device", device.getName())
                    .register(registry);

            Gauge.builder("network_device_latency_ms", latencyMetrics, m -> m.get(device.getName()))
                    .tag("device", device.getName())
                    .register(registry);
        }
    }

    public List<CheckResult> checkAllDevices() {
        latestResults.clear();

        for (Device device : devices) {
            CheckResult result = checkDevice(device);
            latestResults.add(result);
            deviceStatusMetrics.put(device.getName(), result.isUp() ? 1.0 : 0.0);
            latencyMetrics.put(device.getName(), (double) result.getLatencyMs());
        }
        return latestResults;
    }

    public CheckResult checkDevice(Device device) {
        long start = System.currentTimeMillis();
        boolean up;

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(device.getHost(), device.getPort()), 2000);
            up = true;
        } catch (Exception e) {
            up = false;
        }

        long latency = System.currentTimeMillis() - start;
        return new CheckResult(device.getName(), up, latency, LocalDateTime.now());
    }

    public List<CheckResult> getLatestResults() {
        return latestResults;
    }

    public List<Device> getDevices() {
        return devices;
    }
}