package pl.edu.uksw.java;

import java.time.LocalDateTime;
import java.util.Map;

public class Order {
    private final String id;
    private final Map<String, Integer> items;
    private final LocalDateTime date;
    private final String status;

    public Order(String id, Map<String, Integer> items, LocalDateTime date, String status) {
        this.id = id;
        this.items = Map.copyOf(items);
        this.date = date;
        this.status = status;
    }

    public String getId() { return id; }
    public Map<String, Integer> getItems() { return items; }
    public LocalDateTime getDate() { return date; }
    public String getStatus() { return status; }
}