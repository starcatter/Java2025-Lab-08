package pl.edu.uksw.java;

import java.util.ArrayList;
import java.util.List;

public class User {
    private final String username;
    private final String password;
    private final UserType type;
    private Cart cart = new Cart();
    private final List<Order> orders = new ArrayList<>();

    public User(String username, String password) {
        this(username, password, UserType.USER);
    }

    public User(String username, String password, UserType type) {
        this.username = username;
        this.password = password;
        this.type = type;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public UserType getType() { return type; }
    public Cart getCart() { return cart; }
    public void setCart(Cart cart) { this.cart = cart; }
    public List<Order> getOrders() { return orders; }
}