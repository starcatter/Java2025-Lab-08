package pl.edu.uksw.java;

public class ShoppingCartService {
    private final Database database;
    private final ProductService productService;

    public ShoppingCartService(Database database, ProductService productService) {
        this.database = database;
        this.productService = productService;
    }

    public void addItem(String userId, int productId) {
        Product product = productService.getProductById(productId);
        if (product == null) throw new RuntimeException("Product not found");

        ShoppingCart cart = database.getCart(userId);
        cart.addItem(product);
        database.saveCart(userId, cart);
    }

    public void removeItem(String userId, Product product) {
        ShoppingCart cart = database.getCart(userId);
        cart.removeItem(product);
        database.saveCart(userId, cart);
    }

    public ShoppingCart getCart(String userId) {
        return database.getCart(userId);
    }

    public void clearCart(String userId) {
        database.clearCart(userId);
    }

    public double calculateTotal(String userId) {
        ShoppingCart cart = database.getCart(userId);
        return cart.getItems().entrySet().stream()
                .mapToDouble(entry ->
                        entry.getKey().getPrice() * entry.getValue())
                .sum();
    }
}