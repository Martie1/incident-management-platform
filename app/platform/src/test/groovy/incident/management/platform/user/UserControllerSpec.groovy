package incident.management.platform.user

import com.jayway.jsonpath.JsonPath
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import spock.lang.Specification

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
class UserControllerSpec extends Specification {

	@Autowired
	MockMvc mockMvc

	def "returns users loaded from seed data"() {
		expect:
		mockMvc.perform(get("/api/users/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath('$.name').value("Test User"))
	}

	def "creates and updates a user"() {
		when:
		def created = mockMvc.perform(post("/api/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content('{"name": "Jan Test", "email": "jan@example.com"}'))

		then:
		created.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath('$.id').value(notNullValue()))

		when:
		def location = created.andReturn().response.getHeader("Location")
		def updated = mockMvc.perform(put(location)
				.contentType(MediaType.APPLICATION_JSON)
				.content('{"name": "Jan Zmieniony", "email": "jan@example.com"}'))

		then:
		updated.andExpect(status().isOk())
				.andExpect(jsonPath('$.name').value("Jan Zmieniony"))
	}

	def "rejects invalid user: #reason"() {
		expect:
		mockMvc.perform(post("/api/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isBadRequest())

		where:
		reason          | body
		"blank name"    | '{"name": "", "email": "a@example.com"}'
		"invalid email" | '{"name": "Jan", "email": "not-an-email"}'
		"missing email" | '{"name": "Jan"}'
	}

	def "deletes a user"() {
		given:
		def location = mockMvc.perform(post("/api/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content('{"name": "Do usunięcia", "email": "delete@example.com"}'))
				.andReturn().response.getHeader("Location")

		expect:
		mockMvc.perform(delete(location)).andExpect(status().isNoContent())
		mockMvc.perform(get(location)).andExpect(status().isNotFound())
	}

	def "rejects deleting a user with #incidentStatus incident assigned"() {
		given:
		def userId = createUser("open-$incidentStatus@example.com")
		def incidentId = createIncident(userId, incidentStatus)

		expect:
		mockMvc.perform(delete("/api/users/$userId"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath('$.detail').value("User $userId has 1 open incidents assigned".toString()))
		mockMvc.perform(get("/api/users/$userId")).andExpect(status().isOk())
		mockMvc.perform(get("/api/incidents/$incidentId"))
				.andExpect(jsonPath('$.assigneeId').value(userId))

		where:
		incidentStatus << ["OPEN", "IN_PROGRESS"]
	}

	def "deletes a user and unassigns their resolved incidents"() {
		given:
		def userId = createUser("resolved@example.com")
		def incidentId = createIncident(userId, "RESOLVED")
		def resolvedAt = jsonPathOf(mockMvc.perform(get("/api/incidents/$incidentId")), '$.resolvedAt')

		when:
		def result = mockMvc.perform(delete("/api/users/$userId"))

		then:
		result.andExpect(status().isNoContent())
		mockMvc.perform(get("/api/users/$userId")).andExpect(status().isNotFound())
		mockMvc.perform(get("/api/incidents/$incidentId"))
				.andExpect(jsonPath('$.assigneeId').value(nullValue()))
				.andExpect(jsonPath('$.resolvedAt').value(resolvedAt))
	}

	def "returns 404 when deleting unknown user"() {
		expect:
		mockMvc.perform(delete("/api/users/999999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath('$.detail').value("User 999999 not found"))
	}

	private long createUser(String email) {
		def location = mockMvc.perform(post("/api/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"name": "Assignee", "email": "$email"}"""))
				.andExpect(status().isCreated())
				.andReturn().response.getHeader("Location")
		return location.tokenize('/').last() as long
	}

	private long createIncident(long assigneeId, String incidentStatus) {
		def location = mockMvc.perform(post("/api/incidents")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"title": "Assigned incident", "severity": "LOW", "status": "$incidentStatus", "assigneeId": $assigneeId}"""))
				.andExpect(status().isCreated())
				.andReturn().response.getHeader("Location")
		return location.tokenize('/').last() as long
	}

	private static String jsonPathOf(ResultActions result, String path) {
		return JsonPath.read(result.andReturn().response.contentAsString, path)
	}
}
