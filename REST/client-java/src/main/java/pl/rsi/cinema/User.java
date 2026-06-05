package pl.rsi.cinema;

public class User {
    private int userId;
    private String email;
    private String password;
    private String fullName;

    public User(int userId, String email, String fullName) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
    }

    public User(String email, String password, String fullName) {
        this.userId = 0;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
