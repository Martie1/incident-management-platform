package incident.management.platform.seed

import incident.management.platform.incident.IncidentService
import incident.management.platform.user.UserService
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import spock.lang.Specification
import tools.jackson.core.JacksonException
import tools.jackson.databind.json.JsonMapper

class SeedDataLoaderSpec extends Specification {

	static final String VALID_USERS = '[{"name": "Anna", "email": "anna@example.com"}]'
	static final String VALID_INCIDENTS = '[{"title": "Outage", "severity": "HIGH", "assigneeId": 1}]'

	UserService userService = Mock()
	IncidentService incidentService = Mock()

	def "loads valid users and incidents"() {
		when:
		loader(json(VALID_USERS), json(VALID_INCIDENTS)).run(null)

		then:
		1 * userService.create({ it.name() == "Anna" })
		1 * incidentService.create({ it.title() == "Outage" })
	}

	def "fails on invalid user: #reason"() {
		when:
		def users = """[{"name": "Ok", "email": "ok@example.com"}, $secondUser]"""
		loader(json(users), json(VALID_INCIDENTS)).run(null)

		then:
		def e = thrown(ConstraintViolationException)
		e.message.contains("#2")
		e.message.contains(expectedMessage)
		0 * userService.create(_)
		0 * incidentService.create(_)

		where:
		reason          | secondUser                          || expectedMessage
		"blank name"    | '{"name": " ", "email": "a@x.com"}' || "name"
		"invalid email" | '{"name": "Jan", "email": "nope"}'   || "email"
		"missing email" | '{"name": "Jan"}'                    || "email"
	}

	def "fails on invalid incident: #reason"() {
		when:
		loader(json(VALID_USERS), json(incidents)).run(null)

		then:
		def e = thrown(ConstraintViolationException)
		e.message.contains(expectedMessage)
		0 * userService.create(_)
		0 * incidentService.create(_)

		where:
		reason              | incidents                                                     || expectedMessage
		"missing severity"  | '[{"title": "Outage"}]'                                       || "severity"
		"blank title"       | '[{"title": "", "severity": "LOW"}]'                          || "title"
		"too long title"    | """[{"title": "${'x' * 201}", "severity": "LOW"}]"""          || "title"
		"negative assignee" | '[{"title": "Outage", "severity": "LOW", "assigneeId": -1}]' || "assigneeId"
	}

	def "fails on unparseable data: #reason"() {
		when:
		loader(json(VALID_USERS), json(incidents)).run(null)

		then:
		thrown(JacksonException)
		0 * incidentService.create(_)

		where:
		reason             | incidents
		"malformed json"   | '[{"title": "Outage",'
		"unknown severity" | '[{"title": "Outage", "severity": "HUGE"}]'
		"not a list"       | '{"title": "Outage", "severity": "LOW"}'
	}

	def "fails on null entry"() {
		when:
		loader(json('[null]'), json(VALID_INCIDENTS)).run(null)

		then:
		def e = thrown(IllegalArgumentException)
		e.message.contains("#1")
	}

	def "fails when seed file does not exist"() {
		when:
		loader(new ClassPathResource("seed/does-not-exist.json"), json(VALID_INCIDENTS)).run(null)

		then:
		thrown(FileNotFoundException)
	}

	def "propagates errors from services, e.g. unknown assignee"() {
		given:
		incidentService.create(_) >> { throw new IllegalStateException("Assignee 1 does not exist") }

		when:
		loader(json(VALID_USERS), json(VALID_INCIDENTS)).run(null)

		then:
		thrown(IllegalStateException)
	}

	private SeedDataLoader loader(Resource users, Resource incidents) {
		new SeedDataLoader(
				new SeedProperties(users, incidents),
				JsonMapper.builder().build(),
				Validation.buildDefaultValidatorFactory().validator,
				userService,
				incidentService)
	}

	private static Resource json(String content) {
		new ByteArrayResource(content.bytes)
	}
}
