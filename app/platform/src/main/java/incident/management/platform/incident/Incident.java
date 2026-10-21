package incident.management.platform.incident;

import lombok.Builder;

import java.time.Instant;

@Builder(toBuilder = true)
public record Incident(
		Long id,
		String title,
		String description,
		Severity severity,
		Status status,
		Long assigneeId,
		Instant createdAt,
		Instant resolvedAt
) {}
