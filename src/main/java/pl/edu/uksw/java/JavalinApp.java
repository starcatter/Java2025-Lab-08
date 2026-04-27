package pl.edu.uksw.java;

import io.javalin.Javalin;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.rendering.template.JavalinThymeleaf;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Map;

public class JavalinApp {
    private final int port;
    private final Javalin app;
    private final UserRepository users = new UserRepository();

    public JavalinApp(int port) {
        this.port = port;
        this.app = Javalin.create(config -> {
            // ── Javalin 7: Port configured upfront ──
            config.jetty.port = port;

            // ── Lifecycle Events ──
            config.events.serverStarting(() ->
                    System.out.println("Uruchamianie serwera na porcie " + port + "..."));
            config.events.serverStarted(() ->
                    System.out.println("Serwer gotowy: http://localhost:" + port));

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

            // ── Controllers ──
            var pages   = new PageController();
            var auth    = new AuthController(users);
            var members = new MemberController(users);

            // ── Javalin 7: Routes configured upfront ──
            config.routes.get("/",        pages::home);
            config.routes.get("/about",   pages::about);
            config.routes.get("/contact", pages::contact);

            config.routes.get("/register",  auth::showRegister);
            config.routes.post("/register", auth::handleRegister);
            config.routes.get("/login",     auth::showLogin);
            config.routes.post("/login",    auth::handleLogin);
            config.routes.get("/logout",    auth::handleLogout);

            config.routes.get("/members", members::showMembers);

            // ── Javalin 7: Error & Exception handlers configured upfront ──
            // Order matters: specific exceptions must precede general ones
            config.routes.exception(UnauthorizedResponse.class, (e, ctx) ->
                    ctx.redirect("/login?error=login_required")
            );

            config.routes.error(404, ctx ->
                    ctx.render("error.html", Map.of(
                            "code",    "404",
                            "message", "Nie znaleziono strony: " + ctx.path()
                    ))
            );

            config.routes.exception(Exception.class, (e, ctx) -> {
                e.printStackTrace();
                ctx.status(500).render("error.html", Map.of(
                        "code",    "500",
                        "message", "Wewnętrzny błąd serwera"
                ));
            });
        });
    }

    public void start() {
        app.start(); // Port is already bound via config.jetty.port
    }

    public Javalin getServer() {
        return app;
    }

    public User setupAdminAccount(String username, String password) {
        if (!users.existsByUsername(username)) {
            User admin = new User(username, password, UserType.ADMIN);
            users.add(admin);
            return admin;
        }
        return users.findByUsername(username).orElseThrow();
    }

    private TemplateEngine buildThymeleaf() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}