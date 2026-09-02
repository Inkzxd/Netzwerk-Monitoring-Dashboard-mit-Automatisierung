package de.htwsaar.monitoring.incident;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for accessing and querying monitoring incidents.
 */
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    /**
     * Finds the most recently started incident with the given fingerprint and status.
     *
     * @param fingerprint alert fingerprint used to identify related incidents
     * @param status lifecycle status to filter by
     * @return the latest matching incident, or an empty {@link Optional} if none exists
     */
    Optional<Incident>
    findFirstByFingerprintAndStatusOrderByStartedAtDesc(
            String fingerprint,
            IncidentStatus status
    );

    /**
     * Finds all incidents ordered by start time, newest first.
     *
     * @return all incidents sorted by descending start timestamp
     */
    List<Incident> findAllByOrderByStartedAtDesc();

    /**
     * Finds all incidents with the given status ordered by start time, newest first.
     *
     * @param status lifecycle status to filter by
     * @return matching incidents sorted by descending start timestamp
     */
    List<Incident> findAllByStatusOrderByStartedAtDesc(
            IncidentStatus status
    );
}