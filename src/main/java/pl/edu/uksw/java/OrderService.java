package pl.edu.uksw.java;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class OrderService {
    private final UserRepository userRepo;

    public OrderService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public Order checkout(String username) {
        User user = userRepo.findByUsername(username).orElseThrow();
        if (user.getCart().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }
        Order order = new Order(
            UUID.randomUUID().toString(),
            user.getCart().getItems(),
            LocalDateTime.now(),
            "COMPLETED"
        );
        user.getOrders().add(order);
        user.setCart(new Cart()); // Clear cart after successful checkout
        return order;
    }

    public List<Order> getOrders(String username) {
        return userRepo.findByUsername(username).orElseThrow().getOrders();
    }
}