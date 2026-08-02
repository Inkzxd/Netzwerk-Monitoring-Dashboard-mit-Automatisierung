package de.htwsaar.monitoring.model;

public class Device {
    private String name;
    private String host;
    private int port;
    private boolean enabled;

    public Device(String name, String host, int port, boolean enabled) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.enabled = enabled;
    }

    public String getName() { return name; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public boolean isEnabled() { return enabled; }
}