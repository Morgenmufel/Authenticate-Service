package renatius.authenticationservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.Claims;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import renatius.authenticationservice.dto.JWTAuthenticationDto;
import renatius.authenticationservice.exceptions.InvalidTokenException;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Component
public class JWTService {


    private static final Logger LOGGER = LogManager.getLogger(JWTService.class);
    @Value("${JWT_SECRET}")
    private String jwtSecret;

    public JWTAuthenticationDto generateAuthToken(UUID userid, String username, String email){
        JWTAuthenticationDto jwtDto = new JWTAuthenticationDto();
        jwtDto.setToken(generateJwtToken(userid, username, email));
        jwtDto.setRefreshToken(generateJwtRefreshToken(userid, username, email));
        return jwtDto;
    }

    public JWTAuthenticationDto refreshBaseToken(UUID userid, String username, String email, String refreshToken){
        JWTAuthenticationDto jwtDto = new JWTAuthenticationDto();
        jwtDto.setToken(generateJwtToken(userid, username, email));
        jwtDto.setRefreshToken(refreshToken);
        return jwtDto;
    }

    private String generateJwtToken(UUID userid, String username, String email){
        Date date = Date.from(LocalDateTime.now()
                .plusMinutes(10)
                .atZone(ZoneId.systemDefault())
                .toInstant());

        return Jwts.builder()
                .subject(userid.toString())
                .claim("username", username)
                .claim("email", email)
                .expiration(date)
                .signWith(generateSignKey())
                .compact();
    }

    private String generateJwtRefreshToken(UUID userid, String username, String email){
        Date date = Date.from(LocalDateTime.now()
                .plusDays(5)
                .atZone(ZoneId.systemDefault())
                .toInstant());

        return Jwts.builder()
                .subject(userid.toString())
                .claim("username", username)
                .claim("email", email)
                .expiration(date)
                .signWith(generateSignKey())
                .compact();
    }

    public boolean validateJwtToken(String token) {
        try{
            Jwts.parser()
                    .verifyWith(generateSignKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return true;
        } catch (ExpiredJwtException e) {
            LOGGER.warn("Expired JWT token", e);
            throw new InvalidTokenException("JWT token has expired");
        } catch (UnsupportedJwtException e) {
            LOGGER.warn("Unsupported JWT token", e);
            throw new InvalidTokenException("JWT token type is unsupported");
        } catch (MalformedJwtException e) {
            LOGGER.warn("Malformed JWT token", e);
            throw new InvalidTokenException("JWT token format is invalid");
        } catch (SecurityException e) {
            LOGGER.warn("JWT signature validation failed", e);
            throw new InvalidTokenException("JWT signature is invalid");
        } catch (Exception e) {
            LOGGER.error("Invalid JWT token", e);
            throw new InvalidTokenException("JWT token is invalid");
        }
    }

    private SecretKey generateSignKey(){
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String getEmailFromToken(String token){
        Claims claims = Jwts.parser()
                .verifyWith(generateSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("email", String.class);
    }
}
