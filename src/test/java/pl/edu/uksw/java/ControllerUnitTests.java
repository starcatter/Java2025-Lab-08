package pl.edu.uksw.java;

import io.javalin.Javalin;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.rendering.template.JavalinThymeleaf;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static org.junit.jupiter.api.Assertions.*;

class ControllerUnitTests {
    private Javalin app;
    private UserRepository userRepo;
    private ProductRepository productRepo;
    private CartService cartService;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        // Fresh in-memory state per test
        userRepo = new UserRepository();
        userRepo.add(new User("testuser", "password123"));
        productRepo = new ProductRepository();
        cartService = new CartService(userRepo);
        orderService = new OrderService(userRepo);

        var productsCtrl = new ProductController(productRepo, cartService);
        var cartCtrl     = new CartController(cartService, productRepo);
        var checkoutCtrl = new CheckoutController(cartService, orderService);
        var ordersCtrl   = new OrderController(orderService, productRepo);
        var authCtrl     = new AuthController(userRepo);

        app = Javalin.create(config -> {
            config.jetty.port = 0; // Ephemeral port for testing
            config.fileRenderer(new JavalinThymeleaf(buildThymeleaf()));

            config.routes.get("/login", authCtrl::showLogin);
            config.routes.post("/login", authCtrl::handleLogin);
            config.routes.get("/products", productsCtrl::listProducts);
            config.routes.get("/products/{id}", productsCtrl::showProduct);
            config.routes.post("/cart/add", productsCtrl::addToCart);
            config.routes.get("/cart", cartCtrl::showCart);
            config.routes.get("/checkout", checkoutCtrl::showCheckout);
            config.routes.post("/checkout/process", checkoutCtrl::processCheckout);
            config.routes.get("/orders", ordersCtrl::showOrders);

            // Simplified auth handler for unit tests (returns 401 instead of redirect)
            config.routes.exception(UnauthorizedResponse.class, (e, ctx) ->
                ctx.status(401).result("Unauthorized"));
        });
    }

    @Test
    void testListProductsReturnsOk() {
        JavalinTest.test(app, (server, client) -> {
            var res = client.get("/products");
            assertEquals(200, res.code(), "Products list should return 200");
        });
    }

    @Test
    void testProductDetailNotFound() {
        JavalinTest.test(app, (server, client) -> {
            var res = client.get("/products/nonexistent");
            assertEquals(404, res.code(), "Missing product should return 404");
        });
    }

    @Test
    void testAddToCartRequiresAuthentication() {
        JavalinTest.test(app, (server, client) -> {
            var res = client.post("/cart/add", "productId=p1&qty=1");
            assertEquals(401, res.code(), "Unauthenticated add-to-cart should be rejected");
        });
    }

    @Test
    void testAddToCartSuccessAndRedirect() {
        JavalinTest.test(app, (server, client) -> {
            // Establish session via login
            client.post("/login", "username=testuser&password=password123");
            
            var res = client.post("/cart/add", "productId=p1&qty=2");
            assertEquals(302, res.code(), "Should redirect after adding to cart");
            assertTrue(res.headers().get("Location").getFirst().contains("/cart"), "Redirect target should be /cart");
            
            // Verify service state
            assertEquals(2, cartService.getCart("testuser").getItems().get("p1"),
                "Cart should contain 2 units of p1");
        });
    }

    @Test
    void testCheckoutCreatesOrderAndClearsCart() {
        JavalinTest.test(app, (server, client) -> {
            client.post("/login", "username=testuser&password=password123");
            client.post("/cart/add", "productId=p1&qty=1");
            
            var res = client.post("/checkout/process");
            assertEquals(302, res.code(), "Checkout should redirect");
            System.out.println(res.headers().get("Location"));
            assertTrue(res.headers().get("Location").getFirst().contains("/orders"), "Should redirect to orders page");
            
            // Verify domain state
            assertFalse(cartService.hasItems("testuser"), "Cart should be empty after checkout");
            assertEquals(1, orderService.getOrders("testuser").size(), "One order should be created");
        });
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