package pl.edu.uksw.java;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class RegisterPage {
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