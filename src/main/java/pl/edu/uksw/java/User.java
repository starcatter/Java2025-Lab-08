package pl.edu.uksw.java;

class User {
    private final String username;
    private final String password;
    private final UserType type;

    User(String username, String password) {
        this(username, password, UserType.USER);
    }

    User(String username, String password, UserType type) {
        this.username = username;
        this.password = password;
        this.type = type;
    }

    String getUsername() {
        return username;
    }

    String getPassword() {
        return password;
    }

    UserType getType() {
        return type;
    }
}
