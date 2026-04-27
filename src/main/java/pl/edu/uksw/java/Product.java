package pl.edu.uksw.java;

public class Product {
    private final String id;
    private final String name;
    private final String description;
    private final String imageUrl;
    private final double price;

    public Product(String id, String name, String description, String imageUrl, double price) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.price = price;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public double getPrice() { return price; }
}