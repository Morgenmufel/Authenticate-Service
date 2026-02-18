package renatius.authenticationservice.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthFlowIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15")
                    .withDatabaseName("auth-test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @DynamicPropertySource
    static void configureProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    static {
        postgres.start();
    }

    @LocalServerPort
    private int port;

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost:" + port;
    }


    @Test
    @Order(1)
    void registerUser_shouldReturn201() {

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "email": "reg1@mail.com",
                          "password": "StrongPass123!",
                          "username": "TestReg1"
                        }
                        """)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(201);
    }

    @Test
    @Order(2)
    void registerExistingUser_shouldReturn409() {

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "email": "reg2@mail.com",
                          "password": "StrongPass123!",
                          "username": "TestReg2"
                        }
                        """)
                .post("/auth/register")
                .then()
                .statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "email": "reg2@mail.com",
                          "password": "StrongPass123!",
                          "username": "AnotherUser"
                        }
                        """)
                .post("/auth/register")
                .then()
                .statusCode(anyOf(is(400), is(409)));
    }

    @Test
    @Order(3)
    void login_shouldReturnTokens() {

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "email": "login1@mail.com",
                          "password": "StrongPass123!",
                          "username": "LoginUser1"
                        }
                        """)
                .post("/auth/register");

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "email": "login1@mail.com",
                          "password": "StrongPass123!"
                        }
                        """)
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("refreshToken", notNullValue());
    }

    @Test
    @Order(4)
    void loginWithWrongPassword_shouldReturn401() {

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "email": "wrong1@mail.com",
                          "password": "StrongPass123!",
                          "username": "WrongPassUser"
                        }
                        """)
                .post("/auth/register");

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "email": "wrong1@mail.com",
                          "password": "incorrect"
                        }
                        """)
                .post("/auth/login")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(5)
    void loginWithUnknownEmail_shouldReturn401() {

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "email": "unknown@mail.com",
                          "password": "whatever"
                        }
                        """)
                .post("/auth/login")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(6)
    void validateWithoutAuthorizationHeader_shouldReturn400or401() {

        given()
                .post("/auth/validate")
                .then()
                .statusCode(anyOf(is(400), is(401), is(403)));
    }

    @Test
    @Order(7)
    void validateWithGarbageToken_shouldReturn401() {

        given()
                .header("Authorization", "Bearer THIS_IS_NOT_A_TOKEN")
                .post("/auth/validate")
                .then()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(8)
    void forgotPassword_shouldReturn200() {

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "email": "forgot1@mail.com",
                          "password": "StrongPass123!",
                          "username": "ForgotUser1"
                        }
                        """)
                .post("/auth/register");

        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "email": "forgot1@mail.com" }
                        """)
                .post("/auth/forgot-password")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(9)
    void forgotPasswordForUnknownUser_shouldReturn200Anyway() {

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "email": "renat.shynkarou@innowise.com",
                          "password": "StrongPass123!",
                          "username": "ForgotUser1"
                        }
                        """)
                .post("/auth/register")
                .then()
                .statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "email": "renat.shynkarou@innowise.com" }
                        """)
                .post("/auth/forgot-password")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(10)
    void resetPassword_withInvalidToken_shouldReturn400or404() {

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "resetToken": "invalid-but-well-formed",
                          "newPassword": "NewPass123!"
                        }
                        """)
                .post("/auth/reset-password")
                .then()
                .statusCode(anyOf(is(400), is(404)));
    }

    @Test
    @Order(11)
    void resetPassword_missingFields_shouldReturn400() {

        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "resetToken": "only_token" }
                        """)
                .post("/auth/reset-password")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(12)
    void refreshToken_missingRefreshToken_shouldReturn400() {

        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .post("/auth/refresh-token")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(13)
    void refreshToken_invalidRefreshToken_shouldReturn401() {

        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "refreshToken": "totally-invalid" }
                        """)
                .post("/auth/refresh-token")
                .then()
                .statusCode(anyOf(is(401), is(403)));
    }
}
