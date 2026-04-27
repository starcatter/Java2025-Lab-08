package pl.edu.uksw.java;

import io.javalin.http.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class OrderController {
    private final OrderService orderService;
    private final ProductRepository productRepo;

    OrderController(OrderService orderService, ProductRepository productRepo) {
        this.orderService = orderService;
        this.productRepo = productRepo;
    }

    void showOrders(Context ctx) {
        MemberController.requireLogin(ctx);
        String username = ctx.sessionAttribute("user");
        List<Order> orders = orderService.getOrders(username);

        // Flatten for simpler Thymeleaf rendering
        List<Map<String, Object>> orderModels = new ArrayList<>();
        for (Order o : orders) {
            List<Map<String, Object>> items = new ArrayList<>();
            for (var e : o.getItems().entrySet()) {
                Product p = productRepo.getById(e.getKey()).orElse(null);
                items.add(Map.of("name", p != null ? p.getName() : e.getKey(), "qty", e.getValue()));
            }
            orderModels.add(Map.of(
                "id", o.getId(),
                "date", o.getDate(),
                "status", o.getStatus(),
                "items", items
            ));
        }
        ctx.render("orders.html", Map.of("orders", orderModels));
    }
}