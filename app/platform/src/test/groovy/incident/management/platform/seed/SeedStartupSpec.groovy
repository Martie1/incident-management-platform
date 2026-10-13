package incident.management.platform.seed

import incident.management.platform.PlatformApplication
import jakarta.validation.ConstraintViolationException
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.core.NestedExceptionUtils
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification

class SeedStartupSpec extends Specification {

	def "application does not start with #reason"() {
		when:
		start(users, incidents)

		then:
		def e = thrown(Exception)
		NestedExceptionUtils.getMostSpecificCause(e).class == expectedCause

		where:
		reason              | users                                | incidents                                        || expectedCause
		"invalid user"      | "classpath:seed/invalid-users.json"  | "classpath:seed/test-incidents.json"             || ConstraintViolationException
		"unknown assignee"  | "classpath:seed/test-users.json"     | "classpath:seed/unknown-assignee-incidents.json" || ResponseStatusException
		"missing seed file" | "classpath:seed/does-not-exist.json" | "classpath:seed/test-incidents.json"             || FileNotFoundException
	}

	def "application starts with valid seed files"() {
		when:
		def context = start("classpath:seed/test-users.json", "classpath:seed/test-incidents.json")

		then:
		context.active

		cleanup:
		context?.close()
	}

	private static start(String users, String incidents) {
		new SpringApplicationBuilder(PlatformApplication)
				.web(WebApplicationType.NONE)
				.run("--app.seed.users=$users", "--app.seed.incidents=$incidents")
	}

}
