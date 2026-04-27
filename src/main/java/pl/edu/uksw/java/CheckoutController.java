package pl.edu.uksw.java;

import io.javalin.http.Context;

class CheckoutController {
    private final CartService cartService;
    private final OrderService orderService;

    CheckoutController(CartService cartService, OrderService orderService) {
        this.cartService = cartService;
        this.orderService = orderService;
    }

    void showCheckout(Context ctx) {
        MemberController.requireLogin(ctx);
        if (!cartService.hasItems(ctx.sessionAttribute("user"))) {
            ctx.redirect("/cart");
            return;
        }
        ctx.render("checkout.html");
    }

    void processCheckout(Context ctx) {
        MemberController.requireLogin(ctx);
        orderService.checkout(ctx.sessionAttribute("user"));
        ctx.redirect("/orders?success=true");
    }
}