package pl.edu.uksw.java;

import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MainPageTest extends SeleniumTestBase {
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
        JavalinTest.test(server, (server, client) -> {
            // Navigate to the members page without logging in
            driver.get(client.getOrigin() + "/members");
            String bodyText = driver.findElement(By.tagName("body")).getText();
            assertTrue(bodyText.contains("Forbidden: Login required"), "Unauthorized access allowed to members page");
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
}