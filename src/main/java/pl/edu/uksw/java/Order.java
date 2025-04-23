package pl.edu.uksw.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Order {
    private int id;  
    private String userId;  
    private ShoppingCart cart;  
    private String deliveryAddress;

    // Getters, setters, constructors


    public Order(int id, String userId, ShoppingCart cart, String deliveryAddress) {
        this.id = id;
        this.userId = userId;
        this.cart = cart;
        this.deliveryAddress = deliveryAddress;
    }

    public Order(String userId, ShoppingCart cart, String deliveryAddress) {
        this.userId = userId;
        this.cart = cart;
        this.deliveryAddress = deliveryAddress;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public ShoppingCart getCart() {
        return cart;
    }

    public void setCart(ShoppingCart cart) {
        this.cart = cart;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }
}