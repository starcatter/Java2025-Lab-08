package pl.edu.uksw.java;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Cart {
    private final Map<String, Integer> items = new LinkedHashMap<>();

    public void add(String productId, int quantity) {
        if (quantity <= 0) return;
        items.merge(productId, quantity, Integer::sum);
    }

    public void remove(String productId) {
        items.remove(productId);
    }

    public Map<String, Integer> getItems() {
        return Collections.unmodifiableMap(items);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}