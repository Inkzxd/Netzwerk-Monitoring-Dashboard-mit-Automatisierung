package de.htwsaar.monitoring.incident;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final IncidentRepository incidentRepository;

    public IncidentController(
            IncidentService incidentService,
            IncidentRepository incidentRepository
    ) {
        this.incidentService = incidentService;
        this.incidentRepository = incidentRepository;
    }

    @GetMapping
    public List<Incident> getAll() {
        return incidentService.getAllIncidents();
    }

    @GetMapping("/active")
    public List<Incident> getActive() {
        return incidentService.getActiveIncidents();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Incident> getById(@PathVariable Long id) {
        return incidentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}