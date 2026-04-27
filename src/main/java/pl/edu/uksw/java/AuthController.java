package pl.edu.uksw.java;

import io.javalin.http.Context;
import java.util.Map;

class AuthController {
    private final UserRepository users;

    AuthController(UserRepository users) {
        this.users = users;
    }

    void showRegister(Context ctx) {
        ctx.render("register.html", errorModel(ctx.queryParam("error")));
    }

    void handleRegister(Context ctx) {
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            ctx.redirect("/register?error=empty_fields");
            return;
        }
        if (users.existsByUsername(username)) {
            ctx.redirect("/register?error=username_taken");
            return;
        }

        users.add(new User(username, password));
        ctx.redirect("/login");
    }

    void showLogin(Context ctx) {
        ctx.render("login.html", errorModel(ctx.queryParam("error")));
    }

    void handleLogin(Context ctx) {
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");

        if (users.authenticate(username, password)) {
            ctx.sessionAttribute("user", username);
            ctx.redirect("/");
        } else {
            ctx.redirect("/login?error=invalid_credentials");
        }
    }

    void handleLogout(Context ctx) {
        ctx.sessionAttribute("user", null);
        ctx.redirect("/");
    }

    static Map<String, Object> errorModel(String error) {
        return error != null ? Map.of("error", error) : Map.of();
    }
}