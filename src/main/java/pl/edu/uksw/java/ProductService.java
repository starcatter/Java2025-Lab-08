package pl.edu.uksw.java;

import java.util.List;

public class ProductService {
    private Database db;  
    public ProductService(Database db) { this.db = db; }  
    public void addProduct(Product product) { db.addProduct(product); }

    public List<Product> getAllProducts() {
        return db.getAllProducts();
    }

    public Product getProductById(int id) {
        return db.getProductById(id);
    }
    // ...  
}  