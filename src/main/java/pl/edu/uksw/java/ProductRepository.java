package pl.edu.uksw.java;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ProductRepository {
    private final Map<String, Product> products = new LinkedHashMap<>();

    public ProductRepository() {
        seed();
    }

    private void seed() {
        add(new Product("p1", "Laptop", "High-performance laptop for work and play.", "/img/laptop.png", 999.99));
        add(new Product("p2", "Mouse", "Ergonomic wireless mouse.", "/img/mouse.png", 29.99));
        add(new Product("p3", "Keyboard", "Mechanical keyboard with RGB backlight.", "/img/keyboard.png", 79.99));
    }

    public void add(Product product) {
        products.put(product.getId(), product);
    }

    public Optional<Product> getById(String id) {
        return Optional.ofNullable(products.get(id));
    }

    public Collection<Product> getAll() {
        return products.values();
    }
}