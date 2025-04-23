package pl.edu.uksw.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderService {
    private final Database db;
    private final ShoppingCartService cartService;

    public OrderService(Database db, ShoppingCartService cartService) {
        this.db = db;
        this.cartService = cartService;
    }

    public int placeOrder(String userId, String deliveryAddress) {
        ShoppingCart cart = cartService.getCart(userId);
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        Order order = new Order(userId, cart, deliveryAddress);
        int orderId = db.addOrder(order);
        cartService.clearCart(userId); // Clear cart after order [[3]]
        return orderId;
    }

    public List<Order> getUserOrders(String userId) {
        return db.getOrdersByUser(userId);
    }
}