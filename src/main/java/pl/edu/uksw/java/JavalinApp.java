package pl.edu.uksw.java;

import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinThymeleaf;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavalinApp {
    private final int port;
    final Javalin app;

    private final List<User> users = new ArrayList<>(); // In-memory storage [[7]]

    public JavalinApp(int port) {
        this.port = port;

        TemplateEngine thymeleaf = configureThymeleaf();

        this.app = Javalin.create(config -> {
            // Setup route for images
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/img";
                staticFiles.directory = "/www/static/images";
            });

            // Setup route for css files
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/style";
                staticFiles.directory = "/www/static/css";
            });

            // Log all output
            config.bundledPlugins.enableDevLogging();

            // Enable templating
            config.fileRenderer(new JavalinThymeleaf(thymeleaf));
        });

        // Define routes [[4]]
        app.get("/", ctx -> ctx.render("home.html"));
        app.get("/about", ctx -> ctx.render("about.html"));
        app.get("/contact", ctx -> ctx.render("contact.html"));

        // Setup registration page
        app.get("/register", ctx -> {
            String error = ctx.queryParam("error");
            ctx.render("register.html", error != null
                    ? Map.of("error", error)
                    : new HashMap());
        });

        // Setup registration form handler
        app.post("/register", ctx -> {
            String username = ctx.formParam("username");
            String password = ctx.formParam("password");

            if (users.stream().anyMatch(u -> u.getUsername().equals(username))) {
                ctx.redirect("/register?error=username_taken");
                return;
            }

            users.add(new User(username, password));
            ctx.redirect("/login");
        });

        // Setup login page
        app.get("/login", ctx -> {
            String error = ctx.queryParam("error");
            ctx.render("login.html", error != null
                    ? Map.of("error", error)
                    : new HashMap());
        });

        // Setup login form handler
        app.post("/login", ctx -> {
            String username = ctx.formParam("username");
            String password = ctx.formParam("password");

            boolean isValid = users.stream()
                    .anyMatch(u -> u.getUsername().equals(username) && u.getPassword().equals(password));

            if (isValid) {
                ctx.sessionAttribute("user", username);
                ctx.redirect("/");
            } else {
                ctx.redirect("/login?error=invalid_credentials");
            }
        });

        // Setup logout link
        app.get("/logout", ctx -> {
            ctx.sessionAttribute("user", null); // Clear session [[7]]
            ctx.redirect("/");
        });

        // Setup members page
        app.get("/members", ctx -> {
            // Check if user is logged in [[6]]
            if (ctx.sessionAttribute("user") == null) {
                ctx.status(403).result("Forbidden: Login required");
                return;
            }

            // Extract usernames from in-memory storage [[5]]
            List<String> usernames = users.stream()
                    .map(User::getUsername)
                    .toList();

            // Render members page with user list [[3]]
            ctx.render("members.html", Map.of("users", usernames));
        });
    }

    // Get Thymeleaf instance
    private TemplateEngine configureThymeleaf() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        return templateEngine;
    }

    public User setupAdminAccount(String userName, String password){
        var admin = new User(userName,password, UserType.Admin);
        users.add(admin);
        return admin;
    }

    public void startServer() {
        app.start(port);
    }

}