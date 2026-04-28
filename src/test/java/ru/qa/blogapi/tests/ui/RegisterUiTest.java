package ru.qa.blogapi.tests.ui;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import ru.qa.blogapi.base.BaseApiTest;
import ru.qa.blogapi.base.BaseUiTest;
import ru.qa.blogapi.pages.RegisterPage;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterUiTest extends BaseUiTest {

    @Test
    @Tag("regression")
    @DisplayName("UI /register -> should register user from registration page")
    void shouldRegisterUserFromRegisterPage() {
        String email = randomEmail();
        String password = "SecurePass123!";
        String firstName = "Roman";
        String lastName = "Doe";
        String nickname = "roman_" + suffix(5);
        String phone = randomPhone();
        String birthDate = "1990-01-02";

        RegisterPage registerPage = new RegisterPage(driver);

        //выполняем шаги:
        //открыть страницу
        registerPage.open(uiBaseUrl);
        //ввести email
        registerPage.fillEmail(email);
        //ввести password
        registerPage.fillPassword(password);
        //ввести firstName
        registerPage.fillFirstName(firstName);
        //ввести lastName
        registerPage.fillLastName(lastName);
        //ввести nickname
        registerPage.fillNickname(nickname);
        //ввести phone
        registerPage.fillPhone(phone);
        //ввести birthDate
        registerPage.fillBirthDate(birthDate);
        //клик на кнопку регистрации
        registerPage.clickRegister();

        // Проверяем, что после успешной регистрации произошел редирект на страницу логина
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        assertTrue(wait.until(ExpectedConditions.urlContains("/login")), 
                "Should redirect to login page after successful registration");
    }

    private String randomEmail() {
        return "student_" + suffix(8) + "@example.com";
    }

    private String randomPhone() {
        return "+79" + UUID.randomUUID()
                .toString()
                .replaceAll("[^0-9]", "")
                .substring(0, 9);
    }

    private String suffix(int length) {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, length);
    }
}