package pl.edu.uksw.java;

;

public class User {
    private String username;  
    private String password;
    private UserType type;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        type = UserType.User;
    }

    public User(String username, String password, UserType type) {
        this.username = username;
        this.password = password;
        this.type = type;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}