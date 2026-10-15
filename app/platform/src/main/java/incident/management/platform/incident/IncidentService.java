package incident.management.platform.incident;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import incident.management.platform.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentService {

	private final IncidentRepository repository;
	private final UserService userService;
	private final Clock clock = Clock.systemUTC();

	public List<Incident> findAll(Status status, Severity severity) {
		return repository.findAll().stream()
				.filter(incident -> status == null || incident.status() == status)
				.filter(incident -> severity == null || incident.severity() == severity)
				.toList();
	}

	public Incident findById(long id) {
		return repository.findById(id).orElseThrow(() -> notFound(id));
	}

	public Incident create(IncidentRequest request) {
		validateAssignee(request.assigneeId());
		var now = clock.instant();
		var status = request.status() != null ? request.status() : Status.OPEN;
		var incident = Incident.builder()
				.id(repository.nextId())
				.title(request.title())
				.description(request.description())
				.severity(request.severity())
				.status(status)
				.assigneeId(request.assigneeId())
				.createdAt(now)
				.resolvedAt(status == Status.RESOLVED ? now : null)
				.build();
		repository.save(incident);
		log.info("Created incident {} (severity={}, status={}, assignee={})",
				incident.id(), incident.severity(), incident.status(), incident.assigneeId());
		return incident;
	}

	public Incident update(long id, IncidentRequest request) {
		var current = findById(id);
		validateAssignee(request.assigneeId());
		var status = request.status() != null ? request.status() : current.status();
		var incident = Incident.builder()
				.id(id)
				.title(request.title())
				.description(request.description())
				.severity(request.severity())
				.status(status)
				.assigneeId(request.assigneeId())
				.createdAt(current.createdAt())
				.resolvedAt(resolvedAt(current, status))
				.build();
		repository.save(incident);
		if (current.status() != status) {
			log.info("Incident {} status changed {} -> {}", id, current.status(), status);
		}
		log.info("Updated incident {} (severity={}, status={}, assignee={})",
				id, incident.severity(), incident.status(), incident.assigneeId());
		return incident;
	}

	public void delete(long id) {
		if (!repository.deleteById(id)) {
			throw notFound(id);
		}
		log.info("Deleted incident {}", id);
	}

	private Instant resolvedAt(Incident current, Status newStatus) {
		if (newStatus != Status.RESOLVED) {
			return null;
		}
		return current.resolvedAt() != null ? current.resolvedAt() : clock.instant();
	}

	private void validateAssignee(Long assigneeId) {
		if (assigneeId != null && !userService.exists(assigneeId)) {
			log.warn("Rejected incident with unknown assignee {}", assigneeId);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee %d does not exist".formatted(assigneeId));
		}
	}

	private static ResponseStatusException notFound(long id) {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident %d not found".formatted(id));
	}

}
