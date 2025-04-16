package pl.edu.uksw.java;

import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RegistrationTest  extends SeleniumTestBase {
        @Test
        public void testMainPageTitle() {
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
    /*

  */
}
