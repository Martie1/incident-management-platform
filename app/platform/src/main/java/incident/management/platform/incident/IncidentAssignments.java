package incident.management.platform.incident;

import incident.management.platform.user.UserAssignments;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Component
@RequiredArgsConstructor
class IncidentAssignments implements UserAssignments {

	private final IncidentRepository repository;

	@Override
	public void releaseAssignments(long userId) {
		var assigned = repository.findByAssigneeId(userId);
		var openCount = assigned.stream()
				.filter(incident -> incident.getStatus() != Status.RESOLVED)
				.count();
		if (openCount > 0) {
			log.warn("Rejected deletion of user {} with {} open incidents", userId, openCount);
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"User %d has %d open incidents assigned".formatted(userId, openCount));
		}
		assigned.forEach(incident -> repository.save(incident.toBuilder().assigneeId(null).build()));
		log.info("Unassigned {} resolved incidents from user {}", assigned.size(), userId);
	}
}
