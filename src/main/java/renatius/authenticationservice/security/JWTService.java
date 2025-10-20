package renatius.authenticationservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import renatius.authenticationservice.dto.JWTAuthenticationDto;
import renatius.authenticationservice.exceptions.InvalidTokenException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
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
                .signWith(generateSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private String generateJwtRefreshToken(UUID userid, String username, String email){
        Date date = Date.from(LocalDateTime.now()
                .plusDays(1)
                .atZone(ZoneId.systemDefault())
                .toInstant());

        return Jwts.builder()
                .subject(userid.toString())
                .claim("username", username)
                .claim("email", email)
                .expiration(date)
                .signWith(generateSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateJwtToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new InvalidTokenException("JWT token is missing");
        }
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
        LOGGER.error("Generate key" + Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)));
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String getUsernameFromToken(String token){
        Claims claims = Jwts.parser()
                .verifyWith(generateSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("username", String.class);
    }

    public UUID getUserIdFromToken(String token){
        Claims claims = Jwts.parser()
                .verifyWith(generateSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return UUID.fromString(claims.getSubject());
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
