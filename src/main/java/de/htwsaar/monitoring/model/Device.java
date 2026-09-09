package de.htwsaar.monitoring.model;

/**
 * Represents a monitored network device.
 *
 * <p>A device has a stable ID used for metric labels and internal references,
 * as well as a human-readable display name.</p>
 */
public class Device {

    /**
     * Stable device identifier. This value should not change when the display
     * name of the device is changed.
     */
    private final String id;

    /**
     * Human-readable device name shown in the dashboard.
     */
    private final String name;

    /**
     * Hostname or IP address used by the TCP connectivity check.
     */
    private final String host;

    /**
     * TCP port used for the connectivity check.
     */
    private final int port;

    /**
     * Indicates whether the device is included in scheduled checks.
     */
    private final boolean enabled;

    /**
     * Creates a monitored network device.
     *
     * @param id stable identifier used by metrics and internal references
     * @param name display name shown to users
     * @param host hostname or IP address to check
     * @param port TCP port to check
     * @param enabled whether scheduled checks are enabled
     */
    public Device(
            String id,
            String name,
            String host,
            int port,
            boolean enabled
    ) {
        this.id = id;
        this.name = name;
        this.host = host;
        this.port = port;
        this.enabled = enabled;
    }

    /**
     * Returns the stable device identifier.
     *
     * @return device ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the display name.
     *
     * @return device display name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the hostname or IP address.
     *
     * @return device host
     */
    public String getHost() {
        return host;
    }

    /**
     * Returns the monitored TCP port.
     *
     * @return device TCP port
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns whether the device is enabled for monitoring.
     *
     * @return true if enabled, otherwise false
     */
    public boolean isEnabled() {
        return enabled;
    }
}