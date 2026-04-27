package pl.edu.uksw.java;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class UserRepository {
    private final List<User> users = new ArrayList<>();

    void add(User user) {
        users.add(user);
    }

    boolean existsByUsername(String username) {
        return users.stream()
                .anyMatch(u -> u.getUsername().equals(username));
    }

    boolean authenticate(String username, String password) {
        return users.stream().anyMatch(u ->
                u.getUsername().equals(username) &&
                        u.getPassword().equals(password));
    }

    List<String> allUsernames() {
        return users.stream().map(User::getUsername).toList();
    }

    public Optional<User> findByUsername(String username) {
        return users.stream()
                .filter(u -> u.getUsername().equals(username)).findFirst();
    }
}
