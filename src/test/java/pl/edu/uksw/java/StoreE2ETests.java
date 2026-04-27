package pl.edu.uksw.java;

import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StoreE2ETests extends SeleniumTestBase {

    @Test
    void testFullShoppingFlow() {
        JavalinTest.test(server, (srv, client) -> {
            String baseUrl = client.getOrigin();

            // 1. Login
            driver.get(baseUrl + "/login");
            LoginPage loginPage = new LoginPage(driver);
            loginPage.login("admin", "admin");
            assertEquals(baseUrl + "/", driver.getCurrentUrl(), "Should redirect home after login");

            // 2. Browse Products
            driver.get(baseUrl + "/products");
            ProductsPage productsPage = new ProductsPage(driver);
            assertTrue(productsPage.hasProducts(), "Products list should not be empty");
            String firstProductName = productsPage.getFirstProductName();
            productsPage.clickFirstProduct();

            // 3. Product Details & Add to Cart
            ProductDetailPage detailPage = new ProductDetailPage(driver);
            assertEquals(firstProductName, detailPage.getProductName(), "Detail page should show correct product");
            detailPage.addToCart(1);
            assertTrue(driver.getCurrentUrl().contains("/cart"), "Should redirect to cart after adding");

            // 4. Verify Cart
            CartPage cartPage = new CartPage(driver);
            assertTrue(cartPage.containsProduct(firstProductName), "Cart should contain the added product");
            cartPage.proceedToCheckout();

            // 5. Checkout
            CheckoutPage checkoutPage = new CheckoutPage(driver);
            checkoutPage.confirmPurchase();
            assertTrue(driver.getCurrentUrl().contains("/orders"), "Should redirect to orders after checkout");

            // 6. Verify Orders
            OrdersPage ordersPage = new OrdersPage(driver);
            assertTrue(ordersPage.hasOrders(), "Orders list should not be empty");
            assertTrue(ordersPage.getLastOrderContains(firstProductName), "Latest order should contain the product");
        });
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Page Objects
    // ─────────────────────────────────────────────────────────────────────────────

    public static class LoginPage {
        @FindBy(name = "username") private WebElement usernameField;
        @FindBy(name = "password") private WebElement passwordField;
        @FindBy(css = "button[type='submit']") private WebElement submitBtn;

        public LoginPage(WebDriver driver) { PageFactory.initElements(driver, this); }
        public void login(String user, String pass) {
            usernameField.sendKeys(user);
            passwordField.sendKeys(pass);
            submitBtn.click();
        }
    }

    public static class ProductsPage {
        @FindBy(css = "main div h3") private List<WebElement> productNames;
        @FindBy(css = "main div a")  private List<WebElement> productLinks;

        public ProductsPage(WebDriver driver) { PageFactory.initElements(driver, this); }
        public boolean hasProducts() { return !productNames.isEmpty(); }
        public String getFirstProductName() { return productNames.get(0).getText(); }
        public void clickFirstProduct() { productLinks.get(0).click(); }
    }

    public static class ProductDetailPage {
        @FindBy(tagName = "h2") private WebElement nameHeader;
        @FindBy(name = "qty")   private WebElement qtyInput;
        @FindBy(css = "button[type='submit']") private WebElement addBtn;

        public ProductDetailPage(WebDriver driver) { PageFactory.initElements(driver, this); }
        public String getProductName() { return nameHeader.getText(); }
        public void addToCart(int quantity) {
            qtyInput.clear();
            qtyInput.sendKeys(String.valueOf(quantity));
            addBtn.click();
        }
    }

    public static class CartPage {
        @FindBy(css = "main li strong") private List<WebElement> itemNames;
        @FindBy(css = "a[href='/checkout']") private WebElement checkoutLink;

        public CartPage(WebDriver driver) { PageFactory.initElements(driver, this); }
        public boolean containsProduct(String name) {
            return itemNames.stream().anyMatch(el -> el.getText().equals(name));
        }
        public void proceedToCheckout() { checkoutLink.click(); }
    }

    public static class CheckoutPage {
        @FindBy(css = "button[type='submit']") private WebElement confirmBtn;
        public CheckoutPage(WebDriver driver) { PageFactory.initElements(driver, this); }
        public void confirmPurchase() { confirmBtn.click(); }
    }

    public static class OrdersPage {
        @FindBy(css = "main div") private List<WebElement> orderBlocks;
        @FindBy(css = "main div ul li span") private List<WebElement> orderItems;

        public OrdersPage(WebDriver driver) { PageFactory.initElements(driver, this); }
        public boolean hasOrders() { return !orderBlocks.isEmpty(); }
        public boolean getLastOrderContains(String productName) {
            // Simplified: checks if any item in the rendered orders matches the product
            return orderItems.stream().anyMatch(el -> el.getText().contains(productName));
        }
    }
}