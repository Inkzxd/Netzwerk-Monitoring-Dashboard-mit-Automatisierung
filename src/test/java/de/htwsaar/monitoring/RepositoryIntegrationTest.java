package de.htwsaar.monitoring;

import de.htwsaar.monitoring.incident.Incident;
import de.htwsaar.monitoring.incident.IncidentRepository;
import de.htwsaar.monitoring.incident.IncidentStatus;
import de.htwsaar.monitoring.model.CheckResultEntity;
import de.htwsaar.monitoring.model.CheckResultRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RepositoryIntegrationTest {

    @Autowired
    private CheckResultRepository checkResultRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Test
    void shouldSaveAndReadCheckResult() {
        CheckResultEntity entity = new CheckResultEntity(
                "Router",
                "192.0.2.1",
                true,
                15,
                LocalDateTime.now(),
                null
        );

        CheckResultEntity saved = checkResultRepository.save(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDeviceName()).isEqualTo("Router");
        assertThat(saved.isUp()).isTrue();
        assertThat(saved.getLatencyMs()).isEqualTo(15);

        Optional<CheckResultEntity> found = checkResultRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getDeviceName()).isEqualTo("Router");
    }

    @Test
    void shouldFindCheckResultsByDeviceName() {
        CheckResultEntity entity1 = new CheckResultEntity(
                "Router",
                "192.0.2.1",
                true,
                10,
                LocalDateTime.now(),
                null
        );

        CheckResultEntity entity2 = new CheckResultEntity(
                "Router",
                "192.0.2.1",
                false,
                0,
                LocalDateTime.now().minusMinutes(5),
                "timeout"
        );

        checkResultRepository.save(entity1);
        checkResultRepository.save(entity2);

        List<CheckResultEntity> results = checkResultRepository.findByDeviceNameOrderByCheckedAtDesc("Router");

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getCheckedAt()).isAfterOrEqualTo(results.get(1).getCheckedAt());
    }

    @Test
    void shouldFindCheckResultsByTimeRange() {
        LocalDateTime now = LocalDateTime.now();

        CheckResultEntity oldEntity = new CheckResultEntity(
                "Old Device",
                "192.0.2.100",
                false,
                0,
                now.minusDays(40),
                "timeout"
        );

        CheckResultEntity newEntity = new CheckResultEntity(
                "New Device",
                "192.0.2.101",
                true,
                5,
                now.minusDays(10),
                null
        );

        checkResultRepository.save(oldEntity);
        checkResultRepository.save(newEntity);

        LocalDateTime cutoff = now.minusDays(30);
        List<CheckResultEntity> after = checkResultRepository.findByCheckedAtAfterOrderByCheckedAtDesc(cutoff);

        assertThat(after).hasSize(1);
        assertThat(after.get(0).getDeviceName()).isEqualTo("New Device");

        List<CheckResultEntity> before = checkResultRepository.findByCheckedAtBefore(cutoff);
        assertThat(before).hasSize(1);
        assertThat(before.get(0).getDeviceName()).isEqualTo("Old Device");
    }

    @Test
    void shouldSaveAndReadIncident() {
        Incident incident = new Incident(
                "fp-123",
                "NetworkDeviceDown",
                "Router",
                "192.0.2.1",
                "critical",
                OffsetDateTime.now().minusMinutes(10),
                "Device is down",
                "Router unreachable"
        );

        Incident saved = incidentRepository.save(incident);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFingerprint()).isEqualTo("fp-123");
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.FIRING);
        assertThat(saved.getDeviceName()).isEqualTo("Router");

        Optional<Incident> found = incidentRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getAlertName()).isEqualTo("NetworkDeviceDown");
    }

    @Test
    void shouldFindIncidentByFingerprintAndStatus() {
        Incident incident = new Incident(
                "fp-search",
                "NetworkDeviceDown",
                "Switch",
                "192.0.2.2",
                "warning",
                OffsetDateTime.now().minusMinutes(5),
                "High latency",
                "Latency above threshold"
        );

        incidentRepository.save(incident);

        Optional<Incident> found = incidentRepository.findFirstByFingerprintAndStatusOrderByStartedAtDesc(
                "fp-search",
                IncidentStatus.FIRING
        );

        assertThat(found).isPresent();
        assertThat(found.get().getDeviceName()).isEqualTo("Switch");
        assertThat(found.get().getStatus()).isEqualTo(IncidentStatus.FIRING);
    }

    @Test
    void shouldFindAllIncidentsByStatus() {
        Incident firing1 = new Incident(
                "fp-f1",
                "Alert1",
                "Device1",
                "192.0.2.10",
                "critical",
                OffsetDateTime.now().minusMinutes(2),
                "Firing 1",
                "Description 1"
        );

        Incident firing2 = new Incident(
                "fp-f2",
                "Alert2",
                "Device2",
                "192.0.2.20",
                "warning",
                OffsetDateTime.now().minusMinutes(1),
                "Firing 2",
                "Description 2"
        );

        Incident resolved = new Incident(
                "fp-r1",
                "Alert3",
                "Device3",
                "192.0.2.30",
                "critical",
                OffsetDateTime.now().minusMinutes(10),
                "Resolved",
                "Already fixed"
        );

        resolved.resolve(OffsetDateTime.now().minusMinutes(5));

        incidentRepository.save(firing1);
        incidentRepository.save(firing2);
        incidentRepository.save(resolved);

        List<Incident> firingIncidents = incidentRepository.findAllByStatusOrderByStartedAtDesc(IncidentStatus.FIRING);

        assertThat(firingIncidents).hasSize(2);
        assertThat(firingIncidents.get(0).getStartedAt()).isAfterOrEqualTo(firingIncidents.get(1).getStartedAt());

        List<Incident> resolvedIncidents = incidentRepository.findAllByStatusOrderByStartedAtDesc(IncidentStatus.RESOLVED);
        assertThat(resolvedIncidents).hasSize(1);
    }

    @Test
    void shouldFindAllIncidentsOrderedByStartTime() {
        Incident incident1 = new Incident(
                "fp-1",
                "Alert1",
                "Device1",
                "192.0.2.1",
                "critical",
                OffsetDateTime.now().minusMinutes(5),
                "First",
                "First incident"
        );

        Incident incident2 = new Incident(
                "fp-2",
                "Alert2",
                "Device2",
                "192.0.2.2",
                "warning",
                OffsetDateTime.now().minusMinutes(2),
                "Second",
                "Second incident"
        );

        incidentRepository.save(incident1);
        incidentRepository.save(incident2);

        List<Incident> all = incidentRepository.findAllByOrderByStartedAtDesc();

        assertThat(all).hasSize(2);
        assertThat(all.get(0).getStartedAt()).isAfterOrEqualTo(all.get(1).getStartedAt());
    }
}