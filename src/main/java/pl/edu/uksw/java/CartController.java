package pl.edu.uksw.java;

import io.javalin.http.Context;
import java.util.LinkedHashMap;
import java.util.Map;

class CartController {
    private final CartService cartService;
    private final ProductRepository productRepo;

    CartController(CartService cartService, ProductRepository productRepo) {
        this.cartService = cartService;
        this.productRepo = productRepo;
    }

    void showCart(Context ctx) {
        MemberController.requireLogin(ctx);
        String username = ctx.sessionAttribute("user");
        Cart cart = cartService.getCart(username);

        Map<Product, Integer> cartItems = new LinkedHashMap<>();
        for (var entry : cart.getItems().entrySet()) {
            productRepo.getById(entry.getKey()).ifPresent(p -> cartItems.put(p, entry.getValue()));
        }
        ctx.render("cart.html", Map.of("cartItems", cartItems));
    }

    void removeFromCart(Context ctx) {
        MemberController.requireLogin(ctx);
        String productId = ctx.formParam("productId");
        cartService.removeFromCart(ctx.sessionAttribute("user"), productId);
        ctx.redirect("/cart");
    }
}