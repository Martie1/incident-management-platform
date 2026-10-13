package incident.management.platform.seed;

import java.io.IOException;
import java.util.List;

import incident.management.platform.incident.IncidentRequest;
import incident.management.platform.incident.IncidentService;
import incident.management.platform.user.UserRequest;
import incident.management.platform.user.UserService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import static java.util.stream.Collectors.joining;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(SeedProperties.class)
class SeedDataLoader implements ApplicationRunner {

	private final SeedProperties properties;
	private final JsonMapper jsonMapper;
	private final Validator validator;
	private final UserService userService;
	private final IncidentService incidentService;

	@Override
	public void run(ApplicationArguments args) throws IOException {
		var users = read(properties.users(), new TypeReference<List<UserRequest>>() {});
		var incidents = read(properties.incidents(), new TypeReference<List<IncidentRequest>>() {});

		users.forEach(userService::create);
		incidents.forEach(incidentService::create);

		log.info("Loaded {} users and {} incidents", users.size(), incidents.size());
	}

	private <T> List<T> read(Resource resource, TypeReference<List<T>> type) throws IOException {
		log.info("Loading seed data from {}", resource.getDescription());
		List<T> entries;
		try (var input = resource.getInputStream()) {
			entries = jsonMapper.readValue(input, type);
		}
		for (int i = 0; i < entries.size(); i++) {
			validate(entries.get(i), i, resource);
		}
		return entries;
	}

	private <T> void validate(T entry, int index, Resource resource) {
		if (entry == null) {
			throw new IllegalArgumentException("Seed entry #%d in %s is null".formatted(index + 1, resource.getDescription()));
		}
		var violations = validator.validate(entry);
		if (!violations.isEmpty()) {
			var details = violations.stream()
					.map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
					.sorted()
					.collect(joining(", "));
			throw new ConstraintViolationException(
					"Invalid seed entry #%d in %s: %s".formatted(index + 1, resource.getDescription(), details),
					violations);
		}
	}
}
