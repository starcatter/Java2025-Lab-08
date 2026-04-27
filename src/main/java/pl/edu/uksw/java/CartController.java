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
        double total = 0.0;
        for (var entry : cart.getItems().entrySet()) {
            var optProduct = productRepo.getById(entry.getKey());
            if (optProduct.isPresent()) {
                Product p = optProduct.get();
                cartItems.put(p, entry.getValue());
                total += p.getPrice() * entry.getValue();
            }
        }
        ctx.render("cart.html", Map.of("cartItems", cartItems, "total", total));
    }

    void removeFromCart(Context ctx) {
        MemberController.requireLogin(ctx);
        String productId = ctx.formParam("productId");
        cartService.removeFromCart(ctx.sessionAttribute("user"), productId);
        ctx.redirect("/cart");
    }
}