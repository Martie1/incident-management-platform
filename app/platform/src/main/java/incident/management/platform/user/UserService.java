package incident.management.platform.user;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository repository;

	public List<User> findAll() {
		return repository.findAll();
	}

	public User findById(long id) {
		return repository.findById(id)
				.orElseThrow(() -> notFound(id));
	}

	public boolean exists(long id) {
		return repository.existsById(id);
	}

	public User create(UserRequest request) {
		return repository.insert(request);
	}

	public User update(long id, UserRequest request) {
		findById(id);
		var user = new User(id, request.name(), request.email());
		repository.update(user);
		return user;
	}

	public void delete(long id) {
		if (!repository.deleteById(id)) {
			throw notFound(id);
		}
	}

	private static ResponseStatusException notFound(long id) {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, "User %d not found".formatted(id));
	}
}
