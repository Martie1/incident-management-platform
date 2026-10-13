package incident.management.platform.user;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
class UserController {

	private final UserService service;

	@GetMapping
	List<User> findAll() {
		return service.findAll();
	}

	@GetMapping("/{id}")
	User findById(@PathVariable long id) {
		return service.findById(id);
	}

	@PostMapping
	ResponseEntity<User> create(@Valid @RequestBody UserRequest request) {
		var user = service.create(request);
		return ResponseEntity.created(URI.create("/api/users/" + user.id())).body(user);
	}

	@PutMapping("/{id}")
	User update(@PathVariable long id, @Valid @RequestBody UserRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable long id) {
		service.delete(id);
	}
}
