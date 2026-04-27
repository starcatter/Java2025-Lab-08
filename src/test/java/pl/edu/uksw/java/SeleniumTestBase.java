package pl.edu.uksw.java;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

class SeleniumTestBase {
    WebDriver driver;
    final JavalinApp app;
    final Javalin server;
    final User admin;

    public SeleniumTestBase() {
        // Note: JavalinTest.test() overrides the port and binds to a random free port.
        app = new JavalinApp(8080);
        server = app.getServer();
        admin = app.setupAdminAccount("admin", "admin");
    }

    @BeforeAll
    static void setupAll() {
        WebDriverManager.firefoxdriver().setup();
    }

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