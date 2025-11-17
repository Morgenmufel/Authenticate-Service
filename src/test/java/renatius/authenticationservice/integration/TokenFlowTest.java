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
import renatius.authenticationservice.AuthenticationServiceApplication;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Testcontainers
@SpringBootTest(
        classes = AuthenticationServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TokenFlowTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15")
                    .withDatabaseName("authdb")
                    .withUsername("test")
                    .withPassword("test");

    @LocalServerPort
    private int port;

    private String accessToken;
    private String refreshToken;

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
            registry.add("spring.datasource.url", () -> postgres.getJdbcUrl());
            registry.add("spring.datasource.username", () -> postgres.getUsername());
            registry.add("spring.datasource.password", () -> postgres.getPassword());
            registry.add("JWT_SECRET", () -> "SUPER_SECRET_256BIT_KEY_SUPER_SECRET_256BIT_KEY");
    }

    static {
        postgres.start();
    }

    @BeforeAll
    void setup() {
        RestAssured.port = port;
    }

    @Test
    void fullTokenFlow_shouldWorkCorrectly() {

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "email": "user1@mail.com",
                          "password": "StrongPass123!",
                          "username": "UserTest"
                        }
                        """)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(anyOf(is(200), is(201)));

        var loginResponse =
                given()
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                  "email": "user1@mail.com",
                                  "password": "StrongPass123!"
                                }
                                """)
                        .when()
                        .post("/auth/login")
                        .then()
                        .statusCode(200)
                        .body("token", notNullValue())
                        .body("refreshToken", notNullValue())
                        .extract();

        accessToken = loginResponse.path("token");
        refreshToken = loginResponse.path("refreshToken");

        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .post("/auth/validate")
                .then()
                .statusCode(200)
                .body(equalTo("true"));

        var refreshResponse =
                given()
                        .contentType(ContentType.JSON)
                        .body("{\"refreshToken\": \"" + refreshToken + "\"}")
                        .when()
                        .post("/auth/refresh-token")
                        .then()
                        .statusCode(200)
                        .body("token", notNullValue())
                        .extract();

        String newAccess = refreshResponse.path("token");

        given()
                .header("Authorization", "Bearer " + newAccess)
                .when()
                .post("/auth/validate")
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    @Test
    void refreshShouldFailWhenTokenInvalid() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"refreshToken": "totally_invalid_token"}
                        """)
                .post("/auth/refresh-token")
                .then()
                .statusCode(401);
    }
}
