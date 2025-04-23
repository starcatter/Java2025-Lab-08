package pl.edu.uksw.java;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Database {
    private Map<Integer, Product> products = new HashMap<>();
    private Map<String, User> users = new HashMap<>();  
    private Map<String, ShoppingCart> carts = new HashMap<>();  
    private Map<Integer, Order> orders = new HashMap<>();

    private int orderMaxId = 0;
    private int productMaxId = 0;

    // Products
    public void addProduct(Product product) {
        product.setId(productMaxId++);
        products.put(product.getId(), product);
    }
    public Product getProductById(int id) {
        return products.get(id);
    }
    public List<Product> getAllProducts() {
        return products.values().stream().toList();
    }

    // Cart operations
    public synchronized void saveCart(String userId, ShoppingCart cart) {
        carts.put(userId, cart);
    }

    public ShoppingCart getCart(String userId) {
        return carts.getOrDefault(userId, new ShoppingCart());
    }

    public void clearCart(String userId) {
        carts.remove(userId);
    }

    // Order operations
    public synchronized int addOrder(Order order) {
        order.setId(++orderMaxId);
        orders.put(order.getId(), order);
        return orderMaxId;
    }

    public List<Order> getOrdersByUser(String userId) {
        return orders.values().stream()
                .filter(order -> order.getUserId().equals(userId))
                .collect(Collectors.toList());
    }
}  