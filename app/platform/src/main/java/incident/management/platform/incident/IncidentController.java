package incident.management.platform.incident;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
class IncidentController {

	private final IncidentService service;

	@GetMapping
	List<Incident> findAll(@RequestParam(required = false) Status status,
			@RequestParam(required = false) Severity severity) {
		return service.findAll(status, severity);
	}

	@GetMapping("/{id}")
	Incident findById(@PathVariable long id) {
		return service.findById(id);
	}

	@PostMapping
	ResponseEntity<Incident> create(@Valid @RequestBody IncidentRequest request) {
		var incident = service.create(request);
		return ResponseEntity.created(URI.create("/api/incidents/" + incident.getId())).body(incident);
	}

	@PutMapping("/{id}")
	Incident update(@PathVariable long id, @Valid @RequestBody IncidentRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable long id) {
		service.delete(id);
	}
}
