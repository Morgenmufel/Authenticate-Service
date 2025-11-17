package renatius.authenticationservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import renatius.authenticationservice.dto.JWTAuthenticationDto;
import renatius.authenticationservice.exceptions.InvalidTokenException;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JWTServiceTest {

    private JWTService jwtService;
    private final String SECRET = "MY_TEST_SECRET_MY_TEST_SECRET_256bit_key!!!";

    @BeforeEach
    void setUp() {
        jwtService = new JWTService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", SECRET);
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void generateAuthToken_shouldReturnBothTokens() {
        UUID id = UUID.randomUUID();

        JWTAuthenticationDto dto = jwtService.generateAuthToken(id, "user", "email@mail.com");

        assertThat(dto.getToken()).isNotBlank();
        assertThat(dto.getRefreshToken()).isNotBlank();
    }

    @Test
    void refreshBaseToken_shouldReturnNewAccessToken() {
        UUID id = UUID.randomUUID();

        JWTAuthenticationDto dto = jwtService.refreshBaseToken(
                id, "user", "email@mail.com", "REFRESH123"
        );

        assertThat(dto.getToken()).isNotBlank();
        assertThat(dto.getRefreshToken()).isEqualTo("REFRESH123");
    }

    @Test
    void validateJwtToken_shouldReturnTrue_WhenTokenValid() {
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("email", "user@mail.com")
                .expiration(Date.from(LocalDateTime.now().plusMinutes(5)
                        .atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(signingKey())
                .compact();

        assertThat(jwtService.validateJwtToken(token)).isTrue();
    }

    @Test
    void validateJwtToken_shouldThrow_WhenExpired() {
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("email", "user@mail.com")
                .expiration(Date.from(LocalDateTime.now().minusMinutes(5)
                        .atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(signingKey())
                .compact();

        assertThatThrownBy(() -> jwtService.validateJwtToken(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void validateJwtToken_shouldThrow_WhenMalformed() {
        String malformed = "THIS_IS_NOT_A_JWT";

        assertThatThrownBy(() -> jwtService.validateJwtToken(malformed))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void validateJwtToken_shouldThrow_WhenUnsupported() {
        String token = Jwts.builder()
                .header().add("typ", "JWT").add("alg", "none")
                .and()
                .subject("123")
                .claim("email", "test@mail.com")
                .compact(); // no signature → unsupported

        assertThatThrownBy(() -> jwtService.validateJwtToken(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("unsupported");
    }

    @Test
    void getEmailFromToken_shouldExtractEmail() {
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("email", "me@mail.com")
                .expiration(Date.from(LocalDateTime.now().plusMinutes(10)
                        .atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(signingKey())
                .compact();

        String email = jwtService.getEmailFromToken(token);

        assertThat(email).isEqualTo("me@mail.com");
    }
}

