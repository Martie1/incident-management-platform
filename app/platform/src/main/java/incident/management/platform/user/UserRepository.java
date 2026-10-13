package incident.management.platform.user;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

@Repository
class UserRepository {

	private final Map<Long, User> users = new ConcurrentHashMap<>();
	private final AtomicLong sequence = new AtomicLong();

	List<User> findAll() {
		return users.values().stream()
				.sorted(Comparator.comparing(User::id))
				.toList();
	}

	Optional<User> findById(long id) {
		return Optional.ofNullable(users.get(id));
	}

	boolean existsById(long id) {
		return users.containsKey(id);
	}

	User insert(UserRequest request) {
		var user = new User(sequence.incrementAndGet(), request.name(), request.email());
		users.put(user.id(), user);
		return user;
	}

	void update(User user) {
		users.put(user.id(), user);
	}

	boolean deleteById(long id) {
		return users.remove(id) != null;
	}
}
