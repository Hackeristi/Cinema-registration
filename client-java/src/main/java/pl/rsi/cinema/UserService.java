package pl.rsi.cinema;

import java.util.HashMap;
import java.util.Map;

public class UserService {
    private static UserService instance;
    private final Map<String, User> users;
    private User currentUser;

    private UserService() {
        users = new HashMap<>();
        initializeExampleUsers();
    }

    public static UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    private void initializeExampleUsers() {
        // Przykładowe dane do testowania
        users.put("test@example.com", new User("test@example.com", "test123", "Jan Kowalski"));
        users.put("user@cinema.com", new User("user@cinema.com", "password", "Anna Nowak"));
        users.put("admin@cinema.com", new User("admin@cinema.com", "admin123", "Admin Kino"));
        users.put("a", new User("a.com", "a", "Admin Kino"));

    }

    public boolean login(String email, String password) {
        if (users.containsKey(email)) {
            User user = users.get(email);
            if (user.getPassword().equals(password)) {
                currentUser = user;
                return true;
            }
        }
        return false;
    }

    public boolean register(String email, String password, String fullName) {
        if (users.containsKey(email)) {
            return false; // Email już istnieje
        }
        User newUser = new User(email, password, fullName);
        users.put(email, newUser);
        currentUser = newUser;
        return true;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        currentUser = user;
    }

    public void logout() {
        currentUser = null;
    }
}
