package incident.management.platform.incident;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface IncidentRepository extends JpaRepository<Incident, Long> {

	List<Incident> findByAssigneeId(long assigneeId);
}
