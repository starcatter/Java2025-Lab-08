package pl.edu.uksw.java;

import io.javalin.http.Context;

import java.util.List;
import java.util.Map;

public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    void productList(Context ctx) {
        List<Product> products = productService.getAllProducts();
        ctx.render("productList.html", Map.of("products", products));
    }

    void productDetails(Context ctx) {
        Integer id = Integer.valueOf(ctx.pathParam("id"));
        Product product = productService.getProductById(id);
        ctx.render("product.html", Map.of("product", product));
    }
}
