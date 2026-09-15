const REFRESH_INTERVAL_MS = 10000;

document.addEventListener("DOMContentLoaded", () => {
    refreshDashboard();
    setInterval(refreshDashboard, REFRESH_INTERVAL_MS);
});

async function refreshDashboard() {
    try {
        const [
            devices,
            checks,
            pingChecks,
            activeIncidents,
            incidents
        ] = await Promise.all([
            fetchJson("/api/devices"),
            fetchJson("/api/checks/latest"),
            fetchJson("/api/pings/latest"),
            fetchJson("/api/incidents/active"),
            fetchJson("/api/incidents")
        ]);

        renderSummary(devices, checks, activeIncidents);
        renderDevices(devices, checks, pingChecks);
        renderActiveIncidents(activeIncidents);
        renderIncidentHistory(incidents);

        document.getElementById("last-update").textContent =
            `Last update ${new Date().toLocaleString()}`;
    } catch (error) {
        console.error("Dashboard refresh failed", error);
        showErrorState();
    }
}

async function fetchJson(url) {
    const response = await fetch(url);

    if (!response.ok) {
        throw new Error(`${url} returned HTTP ${response.status}`);
    }

    return response.json();
}

function renderSummary(devices, checks, activeIncidents) {
    const online = checks.filter(check => check.up).length;
    const offline = checks.filter(check => !check.up).length;

    document.getElementById("total-devices").textContent = devices.length;
    document.getElementById("online-devices").textContent = online;
    document.getElementById("offline-devices").textContent = offline;
    document.getElementById("active-incidents").textContent =
        activeIncidents.length;
}

function renderDevices(devices, checks, pingChecks) {
    const body = document.getElementById("device-table-body");

    if (!devices.length) {
        body.innerHTML = emptyRow(6, "No devices configured.");
        return;
    }

    const tcpByDevice = new Map(
        checks.map(check => [check.deviceName, check])
    );

    const icmpByDevice = new Map(
        pingChecks.map(check => [check.deviceName, check])
    );

    body.innerHTML = devices.map(device => {
        const tcp = tcpByDevice.get(device.name);
        const icmp = icmpByDevice.get(device.name);

        if (!tcp && !icmp) {
            return `
                <tr>
                    <td>
                        <strong>${escapeHtml(device.name)}</strong><br>
                        <small>
                            ${escapeHtml(device.host)}:${device.port}
                        </small>
                    </td>
                    <td>
                        <span class="status-badge severity-unknown">
                            NO DATA
                        </span>
                    </td>
                    <td>—</td>
                    <td>
                        <span class="status-badge severity-unknown">
                            NO DATA
                        </span>
                    </td>
                    <td>—</td>
                    <td>—</td>
                </tr>
            `;
        }

        const tcpStatus = tcp
            ? renderStatusBadge(tcp.up)
            : renderNoDataBadge();

        const icmpStatus = icmp
            ? renderStatusBadge(icmp.up)
            : renderNoDataBadge();

        const tcpLatency = tcp
            ? `${tcp.latencyMs} ms`
            : "—";

        const icmpLatency = icmp
            ? `${icmp.latencyMs} ms`
            : "—";

        const lastChecked = formatDate(
            tcp?.checkedAt || icmp?.checkedAt
        );

        return `
            <tr>
                <td>
                    <strong>${escapeHtml(device.name)}</strong><br>
                    <small>
                        ${escapeHtml(device.host)}:${device.port}
                    </small>
                </td>
                <td>${tcpStatus}</td>
                <td>${tcpLatency}</td>
                <td>${icmpStatus}</td>
                <td>${icmpLatency}</td>
                <td>${lastChecked}</td>
            </tr>
        `;
    }).join("");
}

function renderStatusBadge(up) {
    const statusClass = up ? "status-up" : "status-down";
    const statusText = up ? "UP" : "DOWN";

    return `
        <span class="status-badge ${statusClass}">
            ${statusText}
        </span>
    `;
}

function renderNoDataBadge() {
    return `
        <span class="status-badge severity-unknown">
            NO DATA
        </span>
    `;
}

function renderActiveIncidents(incidents) {
    const body = document.getElementById(
        "active-incident-table-body"
    );

    if (!incidents.length) {
        body.innerHTML = emptyRow(5, "No active incidents.");
        return;
    }

    body.innerHTML = incidents.map(incident => `
        <tr>
            <td>
                <span class="severity-badge ${severityClass(incident.severity)}">
                    ${escapeHtml(incident.severity)}
                </span>
            </td>
            <td>${escapeHtml(incident.alertName)}</td>
            <td>${escapeHtml(incident.deviceName)}</td>
            <td>${formatDate(incident.startedAt)}</td>
            <td>
                ${escapeHtml(
        incident.description || incident.summary || "—"
    )}
            </td>
        </tr>
    `).join("");
}

function renderIncidentHistory(incidents) {
    const body = document.getElementById("incident-table-body");

    if (!incidents.length) {
        body.innerHTML = emptyRow(7, "No incident history.");
        return;
    }

    body.innerHTML = incidents.map(incident => `
        <tr>
            <td>
                <span class="severity-badge ${severityClass(incident.severity)}">
                    ${escapeHtml(incident.severity)}
                </span>
            </td>
            <td>${escapeHtml(incident.alertName)}</td>
            <td>${escapeHtml(incident.deviceName)}</td>
            <td>
                <span class="status-badge ${statusClass(incident.status)}">
                    ${escapeHtml(incident.status)}
                </span>
            </td>
            <td>${formatDate(incident.startedAt)}</td>
            <td>${formatDate(incident.resolvedAt)}</td>
            <td>
                ${calculateDuration(
        incident.startedAt,
        incident.resolvedAt
    )}
            </td>
        </tr>
    `).join("");
}

async function runChecks() {
    const button = document.getElementById("run-checks-button");

    button.disabled = true;
    button.textContent = "Checking...";

    try {
        const response = await fetch("/api/checks/run", {
            method: "POST"
        });

        if (!response.ok) {
            throw new Error("Manual check failed");
        }

        await refreshDashboard();
    } catch (error) {
        console.error(error);
        alert("The manual check failed.");
    } finally {
        button.disabled = false;
        button.textContent = "Run checks";
    }
}

function formatDate(value) {
    if (!value) {
        return "—";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return "—";
    }

    return date.toLocaleString();
}

function calculateDuration(startValue, endValue) {
    if (!startValue) {
        return "—";
    }

    const start = new Date(startValue);
    const end = endValue ? new Date(endValue) : new Date();

    if (
        Number.isNaN(start.getTime()) ||
        Number.isNaN(end.getTime())
    ) {
        return "—";
    }

    const seconds = Math.max(
        0,
        Math.floor((end.getTime() - start.getTime()) / 1000)
    );

    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;

    if (minutes === 0) {
        return `${remainingSeconds}s`;
    }

    return `${minutes}m ${remainingSeconds}s`;
}

function severityClass(severity) {
    const normalized = String(severity || "").toLowerCase();

    if (normalized === "critical") {
        return "severity-critical";
    }

    if (normalized === "warning") {
        return "severity-warning";
    }

    return "severity-unknown";
}

function statusClass(status) {
    return String(status || "").toUpperCase() === "FIRING"
        ? "status-firing"
        : "status-resolved";
}

function emptyRow(columnCount, message) {
    return `
        <tr>
            <td colspan="${columnCount}" class="empty-state">
                ${escapeHtml(message)}
            </td>
        </tr>
    `;
}

function showErrorState() {
    document.getElementById("device-table-body").innerHTML =
        emptyRow(6, "Could not load device data.");

    document.getElementById("active-incident-table-body").innerHTML =
        emptyRow(5, "Could not load active incidents.");

    document.getElementById("incident-table-body").innerHTML =
        emptyRow(7, "Could not load incident history.");
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}