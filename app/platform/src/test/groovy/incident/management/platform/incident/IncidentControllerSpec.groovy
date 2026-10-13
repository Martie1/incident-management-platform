package incident.management.platform.incident

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.hamcrest.Matchers.everyItem
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.notNullValue
import static org.hamcrest.Matchers.nullValue
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class IncidentControllerSpec extends Specification {

	@Autowired
	MockMvc mockMvc

	def "returns incidents loaded from seed data"() {
		expect:
		mockMvc.perform(get("/api/incidents"))
				.andExpect(status().isOk())
				.andExpect(jsonPath('$[0].title').value("Test incident"))
	}

	def "filters incidents by status"() {
		expect:
		mockMvc.perform(get("/api/incidents").param("status", "OPEN"))
				.andExpect(status().isOk())
				.andExpect(jsonPath('$[*].status', everyItem(is("OPEN"))))
	}

	def "creates an incident with OPEN status by default"() {
		when:
		def result = mockMvc.perform(post("/api/incidents")
				.contentType(MediaType.APPLICATION_JSON)
				.content('{"title": "Login page down", "severity": "HIGH", "assigneeId": 1}'))

		then:
		result.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath('$.id').value(notNullValue()))
				.andExpect(jsonPath('$.status').value("OPEN"))
				.andExpect(jsonPath('$.createdAt').value(notNullValue()))
				.andExpect(jsonPath('$.resolvedAt').value(nullValue()))
	}

	def "rejects invalid incident: #reason"() {
		expect:
		mockMvc.perform(post("/api/incidents")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isBadRequest())

		where:
		reason                | body
		"blank title"         | '{"title": " ", "severity": "LOW"}'
		"missing severity"    | '{"title": "Something broke"}'
		"unknown severity"    | '{"title": "Something broke", "severity": "HUGE"}'
		"too long title"      | """{"title": "${'x' * 201}", "severity": "LOW"}"""
		"non-existing user"   | '{"title": "Something broke", "severity": "LOW", "assigneeId": 999}'
	}

	def "sets resolvedAt when incident gets resolved and clears it when reopened"() {
		given:
		def id = createIncident()

		when:
		def resolved = update(id, "RESOLVED")

		then:
		resolved.andExpect(status().isOk())
				.andExpect(jsonPath('$.status').value("RESOLVED"))
				.andExpect(jsonPath('$.resolvedAt').value(notNullValue()))

		when:
		def reopened = update(id, "IN_PROGRESS")

		then:
		reopened.andExpect(jsonPath('$.resolvedAt').value(nullValue()))
	}

	def "deletes an incident"() {
		given:
		def id = createIncident()

		when:
		def result = mockMvc.perform(delete("/api/incidents/$id"))

		then:
		result.andExpect(status().isNoContent())
		mockMvc.perform(get("/api/incidents/$id")).andExpect(status().isNotFound())
	}

	def "returns 404 for unknown incident"() {
		expect:
		mockMvc.perform(get("/api/incidents/999999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath('$.detail').value("Incident 999999 not found"))
	}

	def "returns validation errors as problem detail"() {
		expect:
		mockMvc.perform(post("/api/incidents")
				.contentType(MediaType.APPLICATION_JSON)
				.content('{"title": ""}'))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath('$.status').value(400))
	}

	private long createIncident() {
		def location = mockMvc.perform(post("/api/incidents")
				.contentType(MediaType.APPLICATION_JSON)
				.content('{"title": "Temporary incident", "severity": "LOW"}'))
				.andExpect(status().isCreated())
				.andReturn().response.getHeader("Location")
		return location.tokenize('/').last() as long
	}

	private update(long id, String newStatus) {
		mockMvc.perform(put("/api/incidents/$id")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"title": "Temporary incident", "severity": "LOW", "status": "$newStatus"}"""))
	}
}
