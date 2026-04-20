package pl.edu.uksw.java;

import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

/**
 * Base class for Selenium based tests for this Javalin app.
 *
 * For best performance, try changing the WebDriver to the Firefox variant,
 * that is change:
 *  firefoxdriver -> firefoxdriver
 *  FirefoxOptions -> FirefoxOprions
 *  FirefoxDriver -> FirefoxDriver
 *
 * and in pom.xml, change:
 * selenium-firefox-driver -> selenium-firefox-driver
 *
 * If neither Firefox nor Firefox works, on Windows you can try using EdgeDriver
 */
class SeleniumTestBase {
    WebDriver driver;

    final JavalinApp app;
    final Javalin server;
    final User admin;

    public SeleniumTestBase() {
        app = new JavalinApp(8080);
        server = app.getServer();
        admin = app.setupAdminAccount("admin","admin");
    }

    @BeforeAll
    static void setupAll() { WebDriverManager.firefoxdriver().setup(); }

    @BeforeEach
    void setupDriver() {
        FirefoxOptions options = new FirefoxOptions();

        options.addArguments("--headless");
        options.addArguments("--disable-gpu");

        driver = new FirefoxDriver(options);
    }

    @AfterEach
    void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }
}

public class ApplicationTests extends SeleniumTestBase {
    @Test
    public void testMainPageTitle() {
        JavalinTest.test(server, (server, client) -> {
            // Navigate to the main page
            driver.get(client.getOrigin() + "/");
            String title = driver.getTitle();
            assertEquals(title, "Javalin App", "Title mismatch on main page");
        });
    }

    @Test
    public void testMembersPageAccessDenied() {
        JavalinTest.test(server, (srv, client) -> {
            // Navigate to the members page without logging in
            driver.get(client.getOrigin() + "/members");
            String bodyText = driver.findElement(By.tagName("body")).getText();
            assertTrue(bodyText.contains("Forbidden: login required"), "Unauthorized access allowed to members page");

            // Verify HTTP status code via Javalin's test client (NOT simulated in browser!)
            var response = client.get("/members");
            assertEquals(403, response.getCode());
        });
    }

    @Test
    public void testLoginWithValidCredentials() {
        JavalinTest.test(server, (server, client) -> {
            // Navigate to the login page
            driver.get(client.getOrigin() + "/login");

            // Fill in login form
            WebElement usernameField = driver.findElement(By.name("username"));
            WebElement passwordField = driver.findElement(By.name("password"));
            WebElement submitButton = driver.findElement(By.cssSelector("button[type='submit']"));

            usernameField.sendKeys(admin.getUsername());
            passwordField.sendKeys(admin.getPassword());
            submitButton.click();

            // Verify redirection to home page
            String currentUrl = driver.getCurrentUrl();
            assertEquals(currentUrl, client.getOrigin() + "/", "Login failed or incorrect redirection");
        });
    }

    @Test
    public void testRegistrationFlow() {
        JavalinTest.test(server, (server, client) -> {
            // Navigate to the registration page
            driver.get(client.getOrigin() + "/register");

            // Fill in registration form
            WebElement usernameField = driver.findElement(By.name("username"));
            WebElement passwordField = driver.findElement(By.name("password"));
            WebElement submitButton = driver.findElement(By.cssSelector("button[type='submit']"));

            String uniqueUsername = "testuser" + System.currentTimeMillis();
            usernameField.sendKeys(uniqueUsername);
            passwordField.sendKeys("password123");
            submitButton.click();

            // Verify redirection to login page
            String currentUrl = driver.getCurrentUrl();
            assertEquals(currentUrl, client.getOrigin() + "/login", "Registration failed or incorrect redirection");
        });
    }

    public static class RegisterPage {
        private WebDriver driver;

        // Locators
        @FindBy(name = "username")
        private WebElement usernameField;

        @FindBy(name = "password")
        private WebElement passwordField;

        @FindBy(css = "button[type='submit']")
        private WebElement submitButton;

        // Constructor
        public RegisterPage(WebDriver driver) {
            this.driver = driver;
            PageFactory.initElements(driver, this);
        }

        // Actions
        public void register(String username, String email, String password) {
            usernameField.sendKeys(username);
            passwordField.sendKeys(password);
            submitButton.click();
        }

        public String getUrl() {
            return driver.getCurrentUrl();
        }
    }

    @Test
    public void testRegisterPageTitle() {
        JavalinTest.test(server, (server, client) -> {
            // generate user name
            String uniqueUsername = "testuser" + System.currentTimeMillis();

            // Navigate to the register page
            driver.get(client.getOrigin() + "/register");

            var registerPage = new RegisterPage(driver);
            registerPage.register(uniqueUsername, uniqueUsername + "@example.com", "password123");

            assertEquals(registerPage.getUrl(), client.getOrigin() + "/login", "Registration failed or incorrect redirection");
        });
    }
}