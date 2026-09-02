package de.htwsaar.monitoring.model;

/**
 * Represents a monitored network device.
 */
public class Device {

    /**
     * Human-readable device name.
     */
    private String name;

    /**
     * Hostname or IP address of the device.
     */
    private String host;

    /**
     * Network port used to check the device.
     */
    private int port;

    /**
     * Indicates whether monitoring is enabled for this device.
     */
    private boolean enabled;

    /**
     * Creates a new monitored device.
     *
     * @param name human-readable device name
     * @param host hostname or IP address of the device
     * @param port network port used to check the device
     * @param enabled whether monitoring is enabled for this device
     */
    public Device(String name, String host, int port, boolean enabled) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.enabled = enabled;
    }

    /**
     * Returns the device name.
     *
     * @return human-readable device name
     */
    public String getName() { return name; }

    /**
     * Returns the device host.
     *
     * @return hostname or IP address
     */
    public String getHost() { return host; }

    /**
     * Returns the device port.
     *
     * @return network port
     */
    public int getPort() { return port; }

    /**
     * Returns whether monitoring is enabled for this device.
     *
     * @return {@code true} if monitoring is enabled, otherwise {@code false}
     */
    public boolean isEnabled() { return enabled; }
}