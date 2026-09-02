package de.htwsaar.monitoring.incident;

/**
 * Lifecycle status of a monitoring incident.
 */
public enum IncidentStatus {

    /**
     * The incident is currently active.
     */
    FIRING,

    /**
     * The incident has been resolved.
     */
    RESOLVED
}