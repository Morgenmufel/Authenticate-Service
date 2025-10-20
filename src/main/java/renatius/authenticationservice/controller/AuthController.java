package renatius.authenticationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import renatius.authenticationservice.dto.JWTAuthenticationDto;
import renatius.authenticationservice.dto.RefreshTokenDto;
import renatius.authenticationservice.dto.UserCredentialsDto;
import renatius.authenticationservice.service.UserService;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<JWTAuthenticationDto> singIn(@RequestBody UserCredentialsDto userCredentialsDto) {
        try {
            JWTAuthenticationDto jwtAuthenticationDto = userService.singIn(userCredentialsDto);
            return ResponseEntity.ok(jwtAuthenticationDto);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    @PostMapping("/refresh-token")
    public JWTAuthenticationDto refresh(@RequestBody RefreshTokenDto refreshTokenDto) throws Exception {
        return userService.refreshToken(refreshTokenDto);
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validate(@RequestHeader("Authorization")  String authHeader) throws Exception {
        String token = authHeader.substring(7);
        if (!userService.validateUserToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "reason", "Invalid or expired token"));
        }

        return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                "valid", true
        ));
    }
}