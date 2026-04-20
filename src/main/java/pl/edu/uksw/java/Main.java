package pl.edu.uksw.java;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.rendering.template.JavalinThymeleaf;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.*;

// ─── Domain ──────────────────────────────────────────────────────────────────

enum UserType { USER, ADMIN }

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
        this.type     = type;
    }

    String getUsername() { return username; }
    String getPassword() { return password; }
    UserType getType()   { return type; }
}

// ─── In-memory storage ───────────────────────────────────────────────────────

class UserRepository {
    private final List<User> users = new ArrayList<>();

    void add(User user) { users.add(user); }

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
}

// ─── Application ─────────────────────────────────────────────────────────────

class JavalinApp {
    private final int port;
    private final Javalin app;
    private final UserRepository users = new UserRepository();

    JavalinApp(int port) {
        this.port = port;
        this.app = Javalin.create(config -> {
            // ── Infrastructure ──
            config.staticFiles.add(sf -> {
                sf.hostedPath = "/img";
                sf.directory  = "/www/static/images";
            });
            config.staticFiles.add(sf -> {
                sf.hostedPath = "/style";
                sf.directory  = "/www/static/css";
            });
            config.bundledPlugins.enableDevLogging();
            config.fileRenderer(new JavalinThymeleaf(buildThymeleaf()));

            // ── Routes ──

            // Static pages
            config.routes.get("/",        ctx -> ctx.render("home.html"));
            config.routes.get("/about",   ctx -> ctx.render("about.html"));
            config.routes.get("/contact", ctx -> ctx.render("contact.html"));

            // Registration
            config.routes.get ("/register", this::showRegister);
            config.routes.post("/register", this::handleRegister);

            // Login / Logout
            config.routes.get ("/login",  this::showLogin);
            config.routes.post("/login",  this::handleLogin);
            config.routes.get ("/logout", this::handleLogout);

            // Protected
            config.routes.get("/members", this::showMembers);
        });
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    private void showRegister(Context ctx) {
        ctx.render("register.html", errorModel(ctx.queryParam("error")));
    }

    private void handleRegister(Context ctx) {
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");

        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
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

    private void showLogin(Context ctx) {
        ctx.render("login.html", errorModel(ctx.queryParam("error")));
    }

    private void handleLogin(Context ctx) {
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");

        if (users.authenticate(username, password)) {
            ctx.sessionAttribute("user", username);
            ctx.redirect("/");
        } else {
            ctx.redirect("/login?error=invalid_credentials");
        }
    }

    private void handleLogout(Context ctx) {
        ctx.sessionAttribute("user", null);
        ctx.redirect("/");
    }

    private void showMembers(Context ctx) {
        if (ctx.sessionAttribute("user") == null) {
            ctx.status(403).result("Forbidden: login required");
            return;
        }
        ctx.render("members.html", Map.of("users", users.allUsernames()));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static Map<String, Object> errorModel(String error) {
        return error != null ? Map.of("error", error) : Map.of();
    }

    private static TemplateEngine buildThymeleaf() {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        var engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    // ── Public API ───────────────────────────────────────────────────────────

    public User setupAdminAccount(String userName, String password){
        var admin = new User(userName,password, UserType.ADMIN);
        users.add(admin);
        return admin;
    }

    void start() {
        app.start(port);
    }

    // ── Test helper  ───────────────────────────────────-─────────────────────

    public Javalin getServer() {
        return app;
    }
}

// ─── Entry point ─────────────────────────────────────────────────────────────

public class Main {
    public static void main(String[] args) {
        var app = new JavalinApp(8089);
        app.setupAdminAccount("admin", "admin");
        app.start();
    }
}