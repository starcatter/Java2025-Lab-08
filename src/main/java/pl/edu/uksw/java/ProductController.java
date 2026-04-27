package pl.edu.uksw.java;

import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import java.util.Map;

class ProductController {
    private final ProductRepository productRepo;
    private final CartService cartService;

    ProductController(ProductRepository productRepo, CartService cartService) {
        this.productRepo = productRepo;
        this.cartService = cartService;
    }

    void listProducts(Context ctx) {
        ctx.render("products.html", Map.of("products", productRepo.getAll()));
    }

    void showProduct(Context ctx) {
        String id = ctx.pathParam("id");
        Product product = productRepo.getById(id)
            .orElseThrow(() -> new NotFoundResponse("Product not found"));
        ctx.render("product-detail.html", Map.of("product", product));
    }

    void addToCart(Context ctx) {
        MemberController.requireLogin(ctx);
        String username = ctx.sessionAttribute("user");
        String productId = ctx.formParam("productId");
        // Javalin 7: nullable validation API
        Integer qty = ctx.formParamAsClass("qty", Integer.class).getOrNull();
        cartService.addToCart(username, productId, qty != null ? qty : 1);
        ctx.redirect("/cart");
    }
}