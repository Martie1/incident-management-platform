package incident.management.platform.incident;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

@Repository
class IncidentRepository {

	private final Map<Long, Incident> incidents = new ConcurrentHashMap<>();
	private final AtomicLong sequence = new AtomicLong();

	List<Incident> findAll() {
		return incidents.values().stream()
				.sorted(Comparator.comparing(Incident::id))
				.toList();
	}

	List<Incident> findByAssigneeId(long assigneeId) {
		return incidents.values().stream()
				.filter(incident -> incident.assigneeId() != null && incident.assigneeId() == assigneeId)
				.toList();
	}

	Optional<Incident> findById(long id) {
		return Optional.ofNullable(incidents.get(id));
	}

	long nextId() {
		return sequence.incrementAndGet();
	}

	void save(Incident incident) {
		incidents.put(incident.id(), incident);
	}

	boolean deleteById(long id) {
		return incidents.remove(id) != null;
	}
}
