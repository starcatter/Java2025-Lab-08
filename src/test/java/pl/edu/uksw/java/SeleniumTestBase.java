package pl.edu.uksw.java;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

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
public class SeleniumTestBase {
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