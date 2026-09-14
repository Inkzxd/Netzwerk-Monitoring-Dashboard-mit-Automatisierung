# LAN/VPN Endpoint Test Guide

## 1. Configure targets

Edit:

```text
src/main/resources/application.yml
```

Use the real LAN gateway and the school VPN DNS server:

```yaml
monitoring:
  devices:
    - id: lan-router
      name: LAN Router
      host: 10.30.0.1
      port: 443
      enabled: true

    - id: vpn-endpoint
      name: VPN Endpoint
      host: 134.96.208.36
      port: 53
      enabled: true
```

Notes:

- `10.30.0.1` is the current Mac default gateway, with `route -n get default`.
- `134.96.208.36` is a school VPN DNS server shown by `scutil --dns` and associated with `utun10`.
- Do not use `134.96.212.87`; that is the Mac's VPN client address.

## 2. Test from the Mac

Run these commands in the Mac Terminal:

```bash
route -n get 10.30.0.1
ping -c 3 10.30.0.1
nc -vz -w 2 10.30.0.1 443
```

```bash
route -n get 134.96.208.36
ping -c 3 134.96.208.36
nc -vz -w 2 134.96.208.36 53
```

For the VPN address, the route should use:

```text
interface: utun10
```

The VPN DNS can also be tested with:

```bash
dig @134.96.208.36 www.htwsaar.de
```

A successful `dig` or TCP port 53 test proves that the VPN DNS service is reachable. Ping may still be `DOWN` if ICMP is blocked.

## 3. Restart the application

Run these commands in the project root, where `docker-compose.yml` is located:

```bash
docker compose down
docker compose up --build -d
```

Check the containers:

```bash
docker compose ps
```

Wait about 30 seconds for the Ping check.

## 4. Check application metrics

```bash
curl -s http://localhost:8080/actuator/prometheus \
  | grep -E 'LAN Router|VPN Endpoint'
```

Important metrics:

```text
network_device_up             TCP status
network_device_ping_up        Ping status
network_device_latency_ms     TCP latency
network_device_ping_latency_ms Ping latency
```

Values:

```text
1.0 = UP
0.0 = DOWN
```

## 5. Interpret results

Expected VPN result:

```text
TCP UP
Ping DOWN or UP
```

For `134.96.208.36`, this is valid when DNS queries and TCP port 53 work but ICMP is blocked.

Expected LAN result depends on the real gateway:

```text
TCP UP/DOWN
Ping UP/DOWN
```

Do not change a `DOWN` value manually. Check the address, route, port, firewall, and VPN state.

## 6. Check Grafana

Open:

```text
http://localhost:3000
```

Open `Network Monitoring`, select `Last 15 minutes`, and refresh.

Check:

- `TCP Device Status`
- `TCP Latency`
- `Ping Device Status`
- `ICMP Ping Latency`

The Dashboard should show `LAN Router` and `VPN Endpoint` with explicit `UP` or `DOWN` values.

## 7. Prometheus queries

Open:

```text
http://localhost:9090/graph
```

Run:

```promql
network_device_up{device="LAN Router"}
```

```promql
network_device_up{device="VPN Endpoint"}
```

```promql
network_device_ping_up{device="LAN Router"}
```

```promql
network_device_ping_up{device="VPN Endpoint"}
```

## Result table

| Target | Address | TCP | Ping | Comment |
|---|---|---|---|---|
| LAN Router | `10.30.0.1` | UP/DOWN | UP/DOWN | Test real default gateway |
| VPN Endpoint | `134.96.208.36` | UP/DOWN | UP/DOWN | TCP/DNS may be UP while Ping is DOWN |
