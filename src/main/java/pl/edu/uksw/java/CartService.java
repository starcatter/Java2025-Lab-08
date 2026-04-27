package pl.edu.uksw.java;

public class CartService {
    private final UserRepository userRepo;

    public CartService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public Cart getCart(String username) {
        return userRepo.findByUsername(username).orElseThrow().getCart();
    }

    public void addToCart(String username, String productId, int quantity) {
        getCart(username).add(productId, quantity);
    }

    public void removeFromCart(String username, String productId) {
        getCart(username).remove(productId);
    }

    public boolean hasItems(String username) {
        return !getCart(username).isEmpty();
    }
}