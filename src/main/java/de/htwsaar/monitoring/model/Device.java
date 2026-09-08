package de.htwsaar.monitoring.model;

/**
 * Represents a monitored network device.
 */
public class Device {

    /**
     * Stable identifier for the device.
     */
    private String id;

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
     * @param id stable identifier for the device
     * @param name human-readable device name
     * @param host hostname or IP address of the device
     * @param port network port used to check the device
     * @param enabled whether monitoring is enabled for this device
     */
    public Device(String id, String name, String host, int port, boolean enabled) {
        this.id = id;
        this.name = name;
        this.host = host;
        this.port = port;
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isEnabled() {
        return enabled;
    }
}