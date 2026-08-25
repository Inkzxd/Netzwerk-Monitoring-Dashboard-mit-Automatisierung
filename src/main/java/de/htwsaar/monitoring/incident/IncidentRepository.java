package de.htwsaar.monitoring.incident;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findFirstByFingerprintAndStatusOrderByStartedAtDesc(
            String fingerprint,
            IncidentStatus status
    );

    List<Incident> findAllByOrderByStartedAtDesc();

    List<Incident> findAllByStatusOrderByStartedAtDesc(
            IncidentStatus status
    );
}