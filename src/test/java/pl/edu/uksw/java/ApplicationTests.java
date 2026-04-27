package pl.edu.uksw.java;

import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationTests extends SeleniumTestBase {

    @Test
    void testMainPageTitle() {
        JavalinTest.test(server, (srv, client) -> {
            driver.get(client.getOrigin() + "/");
            assertEquals("Webstore", driver.getTitle(), "Title mismatch on main page");
        });
    }

    @Test
    void testLoginWithValidCredentials() {
        JavalinTest.test(server, (srv, client) -> {
            driver.get(client.getOrigin() + "/login");
            driver.findElement(By.name("username")).sendKeys(admin.getUsername());
            driver.findElement(By.name("password")).sendKeys(admin.getPassword());
            driver.findElement(By.cssSelector("button[type='submit']")).click();

            assertEquals(client.getOrigin() + "/", driver.getCurrentUrl(),
                    "Po zalogowaniu oczekiwano przekierowania na stronę główną");
        });
    }

    @Test
    void testRegistrationFlow() {
        JavalinTest.test(server, (srv, client) -> {
            driver.get(client.getOrigin() + "/register");
            String uniqueUsername = "testuser" + System.currentTimeMillis();
            driver.findElement(By.name("username")).sendKeys(uniqueUsername);
            driver.findElement(By.name("password")).sendKeys("password123");
            driver.findElement(By.cssSelector("button[type='submit']")).click();

            assertEquals(client.getOrigin() + "/login", driver.getCurrentUrl(),
                    "Po rejestracji oczekiwano przekierowania na stronę logowania");
        });
    }

    @Test
    void testMembersPageAccessDenied() {
        JavalinTest.test(server, (srv, client) -> {
            driver.get(client.getOrigin() + "/members");
            assertTrue(driver.getCurrentUrl().contains("/login"),
                    "Nieudane wejście na /members powinno przekierować na /login");
        });
    }

    // ── Page Object Model ──
    public static class LoginPage {
        private final org.openqa.selenium.WebDriver driver;

        @FindBy(name = "username")
        private WebElement usernameField;

        @FindBy(name = "password")
        private WebElement passwordField;

        @FindBy(css = "button[type='submit']")
        private WebElement submitButton;

        @FindBy(css = ".error")
        private WebElement errorMessage;

        public LoginPage(org.openqa.selenium.WebDriver driver) {
            this.driver = driver;
            PageFactory.initElements(driver, this);
        }

        public void login(String username, String password) {
            usernameField.sendKeys(username);
            passwordField.sendKeys(password);
            submitButton.click();
        }

        public boolean hasError() {
            try {
                return errorMessage.isDisplayed();
            } catch (NoSuchElementException e) {
                return false;
            }
        }

        public String getErrorText() {
            return errorMessage.getText();
        }

        public String getCurrentUrl() {
            return driver.getCurrentUrl();
        }
    }

    @Test
    void testLoginWithPageObject() {
        JavalinTest.test(server, (srv, client) -> {
            driver.get(client.getOrigin() + "/login");
            LoginPage loginPage = new LoginPage(driver);
            loginPage.login(admin.getUsername(), admin.getPassword());
            assertEquals(client.getOrigin() + "/", loginPage.getCurrentUrl(),
                    "Po zalogowaniu oczekiwano przekierowania na stronę główną");
        });
    }
}