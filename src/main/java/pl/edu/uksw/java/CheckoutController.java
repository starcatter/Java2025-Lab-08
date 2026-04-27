package pl.edu.uksw.java;

import io.javalin.http.Context;

import java.util.LinkedHashMap;
import java.util.Map;

class CheckoutController {
    private final CartService cartService;
    private final OrderService orderService;
    private final ProductRepository productRepo;

    CheckoutController(CartService cartService, OrderService orderService, ProductRepository productRepo) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.productRepo = productRepo;
    }

    void showCheckout(Context ctx) {
        MemberController.requireLogin(ctx);
        String username = ctx.sessionAttribute("user");
        if (!cartService.hasItems(username)) {
            ctx.redirect("/cart");
            return;
        }

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
        ctx.render("checkout.html", Map.of("cartItems", cartItems, "total", total));
    }

    void processCheckout(Context ctx) {
        MemberController.requireLogin(ctx);
        orderService.checkout(ctx.sessionAttribute("user"));
        ctx.redirect("/orders?success=true");
    }
}