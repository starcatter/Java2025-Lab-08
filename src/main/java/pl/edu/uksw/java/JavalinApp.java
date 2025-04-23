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

        Database db = new Database();
        ProductService productService = new ProductService(db);
        // Pre-populate sample products
        productService.addProduct(new Product(1,"Wireless Headphones", 99.99, "Noise-canceling Bluetooth headphones"));
        productService.addProduct(new Product(2,"Smartwatch", 149.99, "Fitness tracker with heart rate monitoring"));


        ShoppingCartService shoppingCartService = new ShoppingCartService(db, productService);
        OrderService orderService = new OrderService(db, shoppingCartService);

        // Define routes
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

        // products
        var productController = new ProductController(productService);

        app.get("/products", productController::productList);
        app.get("/products/{id}", productController::productDetails);

//        app.get("/products", ctx -> {
//            List<Product> products = productService.getAllProducts();
//            ctx.render("productList.html", Map.of("products", products));
//        });
//
//        app.get("/products/{id}", ctx -> {
//            Integer id = Integer.valueOf(ctx.pathParam("id"));
//            Product product = productService.getProductById(id);
//            ctx.render("product.html", Map.of("product", product));
//        });

        // shopping cart

        app.get("/cart", ctx -> {
            String userId = ctx.sessionAttribute("user");
            ShoppingCart cart = shoppingCartService.getCart(userId);
            ctx.render("cart.html", Map.of("cart", cart));
        });

        app.post("/cart/add", ctx -> {
            String userId = ctx.sessionAttribute("user");
            if (userId == null) {
                ctx.status(403).result("Login required to add items to cart");
                return;
            }

            int productId = Integer.parseInt(ctx.formParam("productId"));

            shoppingCartService.addItem(userId, productId);
            ctx.redirect("/cart"); // Redirect after POST
        });

        app.post("/cart/remove", ctx -> {
            String userId = ctx.sessionAttribute("user");
            if (userId == null) {
                ctx.status(403).result("Login required to modify cart");
                return;
            }

            int productId = Integer.parseInt(ctx.formParam("productId"));
            Product product = productService.getProductById(productId);

            if(product != null) {
                shoppingCartService.removeItem(userId, product);
            }

            ctx.redirect("/cart"); // Redirect back to cart page
        });

        // ----

        // Checkout form (GET)
        app.get("/checkout", ctx -> {
            String userId = ctx.sessionAttribute("user");
            if (userId == null) ctx.redirect("/login");

            ShoppingCart cart = shoppingCartService.getCart(userId);
            ctx.render("checkout.html", Map.of("cart", cart));
        });

        // Place order (POST)
        app.post("/checkout", ctx -> {
            String userId = ctx.sessionAttribute("user");
            if (userId == null) ctx.redirect("/login");

            String deliveryAddress = ctx.formParam("deliveryAddress");

            int orderId = orderService.placeOrder(userId, deliveryAddress);
            ctx.redirect("/orders/"+orderId);
        });

        // Order list
        app.get("/orders", ctx -> {
            String userId = ctx.sessionAttribute("user");
            if (userId == null) ctx.redirect("/login");

            List<Order> orders = orderService.getUserOrders(userId);
            ctx.render("orders.html", Map.of("orders", orders));
        });

        app.get("/orders/{id}", ctx -> {
            String userId = ctx.sessionAttribute("user");
            if (userId == null) ctx.redirect("/login");

            int orderId = Integer.valueOf(ctx.pathParam("id"));
            var orderOpt = orderService.getUserOrders(userId).stream().filter(o->o.getId()==orderId).findFirst();

            Order order = orderOpt.get(); // Add getOrder() method to Database
            ctx.render("order-details.html", Map.of(
                    "order", order,
                    "productService", productService
            ));
        });

        // ----

        app.get("/admin/addProduct", ctx -> {
            // Ensure user is admin [[7]]
            ctx.render("admin-add-product.html");
        });
        app.post("/admin/addProduct", ctx -> {
            // get these from form
            int id = 0;
            String name = "";
            double price = 0;
            String description = "";

            Product product = new Product(id, name, price, description);
            productService.addProduct(product);
            ctx.redirect("/products");
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

    public User setupAdminAccount(String userName, String password) {
        var admin = new User(userName, password, UserType.Admin);
        users.add(admin);
        return admin;
    }

    public void startServer() {
        app.start(port);
    }

    public Javalin getServer() {
        return app;
    }
}