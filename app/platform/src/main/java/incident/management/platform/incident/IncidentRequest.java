package incident.management.platform.incident;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record IncidentRequest(
		@NotBlank @Size(max = 200) String title,
		@Size(max = 2000) String description,
		@NotNull Severity severity,
		Status status,
		@Positive Long assigneeId
) {}
