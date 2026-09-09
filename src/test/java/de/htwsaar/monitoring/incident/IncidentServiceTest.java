package de.htwsaar.monitoring.incident;

import de.htwsaar.monitoring.alert.AlertmanagerWebhookPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    private IncidentService incidentService;

    @BeforeEach
    void setUp() {
        incidentService = new IncidentService(incidentRepository);
    }

    @Test
    void shouldCreateIncidentWhenAlertStartsFiring() {
        AlertmanagerWebhookPayload.Alert alert = alert(
                "firing",
                "fp-001",
                "Test Device",
                "192.0.2.1",
                OffsetDateTime.parse("2026-08-27T10:00:00Z"),
                null
        );

        when(incidentRepository
                .findFirstByFingerprintAndStatusOrderByStartedAtDesc(
                        "fp-001",
                        IncidentStatus.FIRING
                ))
                .thenReturn(Optional.empty());

        incidentService.process(alert);

        ArgumentCaptor<Incident> captor =
                ArgumentCaptor.forClass(Incident.class);

        verify(incidentRepository).save(captor.capture());

        Incident savedIncident = captor.getValue();

        assertEquals("fp-001", savedIncident.getFingerprint());
        assertEquals("NetworkDeviceDown", savedIncident.getAlertName());
        assertEquals("Test Device", savedIncident.getDeviceName());
        assertEquals("192.0.2.1", savedIncident.getHost());
        assertEquals("critical", savedIncident.getSeverity());
        assertEquals(IncidentStatus.FIRING, savedIncident.getStatus());
    }

    @Test
    void shouldIgnoreDuplicateFiringAlert() {
        AlertmanagerWebhookPayload.Alert alert = alert(
                "firing",
                "fp-001",
                "Test Device",
                "192.0.2.1",
                OffsetDateTime.parse("2026-08-27T10:00:00Z"),
                null
        );

        Incident existingIncident = new Incident(
                "fp-001",
                "NetworkDeviceDown",
                "Test Device",
                "192.0.2.1",
                "critical",
                OffsetDateTime.parse("2026-08-27T10:00:00Z"),
                "Network device is down",
                "Device is unreachable"
        );

        when(incidentRepository
                .findFirstByFingerprintAndStatusOrderByStartedAtDesc(
                        "fp-001",
                        IncidentStatus.FIRING
                ))
                .thenReturn(Optional.of(existingIncident));

        incidentService.process(alert);

        verify(incidentRepository, never()).save(any(Incident.class));
    }

    @Test
    void shouldResolveActiveIncident() {
        AlertmanagerWebhookPayload.Alert alert = alert(
                "resolved",
                "fp-001",
                "Test Device",
                "192.0.2.1",
                OffsetDateTime.parse("2026-08-27T10:00:00Z"),
                OffsetDateTime.parse("2026-08-27T10:08:00Z")
        );

        Incident existingIncident = new Incident(
                "fp-001",
                "NetworkDeviceDown",
                "Test Device",
                "192.0.2.1",
                "critical",
                OffsetDateTime.parse("2026-08-27T10:00:00Z"),
                "Network device is down",
                "Device is unreachable"
        );

        when(incidentRepository
                .findFirstByFingerprintAndStatusOrderByStartedAtDesc(
                        "fp-001",
                        IncidentStatus.FIRING
                ))
                .thenReturn(Optional.of(existingIncident));

        incidentService.process(alert);

        assertEquals(IncidentStatus.RESOLVED, existingIncident.getStatus());
        assertEquals(
                OffsetDateTime.parse("2026-08-27T10:08:00Z"),
                existingIncident.getResolvedAt()
        );

        verify(incidentRepository, never()).save(any(Incident.class));
    }

    @Test
    void shouldIgnoreAlertWithoutFingerprint() {
        AlertmanagerWebhookPayload.Alert alert = new AlertmanagerWebhookPayload.Alert(
                "firing",
                Map.of(
                        "alertname", "NetworkDeviceDown",
                        "device", "Test Device",
                        "host", "192.0.2.1",
                        "severity", "critical"
                ),
                Map.of(
                        "summary", "Network device is down",
                        "description", "Device is unreachable"
                ),
                OffsetDateTime.parse("2026-09-09T12:00:00Z"),
                null,
                "http://prometheus:9090/graph",
                null
        );

        incidentService.process(alert);

        verify(incidentRepository, never()).save(any(Incident.class));
    }

    @Test
    void shouldCreateNewIncidentAfterPreviousIncidentWasResolved() {
        AlertmanagerWebhookPayload.Alert alert = alert(
                "firing",
                "fp-001",
                "Test Device",
                "192.0.2.1",
                OffsetDateTime.parse("2026-08-27T11:00:00Z"),
                null
        );

        when(incidentRepository
                .findFirstByFingerprintAndStatusOrderByStartedAtDesc(
                        "fp-001",
                        IncidentStatus.FIRING
                ))
                .thenReturn(Optional.empty());

        incidentService.process(alert);

        verify(incidentRepository).save(any(Incident.class));
    }

    private AlertmanagerWebhookPayload.Alert alert(
            String status,
            String fingerprint,
            String device,
            String host,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt
    ) {
        return new AlertmanagerWebhookPayload.Alert(
                status,
                Map.of(
                        "alertname", "NetworkDeviceDown",
                        "device", device,
                        "host", host,
                        "severity", "critical"
                ),
                Map.of(
                        "summary", "Network device is down",
                        "description", "Device is unreachable"
                ),
                startsAt,
                endsAt,
                "http://prometheus:9090/graph",
                fingerprint
        );
    }
}