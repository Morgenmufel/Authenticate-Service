package renatius.authenticationservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import renatius.authenticationservice.dto.*;
import renatius.authenticationservice.service.PasswordResetService;
import renatius.authenticationservice.service.UserService;

/**
 * Controller responsible for user authentication and registration operations.
 *
 * <p>Handles login, registration, JWT token refresh, and token validation endpoints.
 * All endpoints are accessible under the base path <b>/auth</b>.</p>
 *
 * <p>Uses {@link UserService} to perform business logic.</p>
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Operations for user login, registration, and token management")
public class AuthController {
    private final UserService userService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates the user and returns JWT tokens")
    public ResponseEntity<JWTAuthenticationDto> singIn(@RequestBody @Valid UserCredentialsDto userCredentialsDto) {
        return ResponseEntity.ok(userService.singIn(userCredentialsDto));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh JWT token", description = "Generates new JWT tokens using a valid refresh token")
    public ResponseEntity<JWTAuthenticationDto> refresh(@RequestBody @Valid RefreshTokenDto refreshTokenDto) {
       JWTAuthenticationDto jwtAuthenticationDto = userService.refreshToken(refreshTokenDto);
       return ResponseEntity.ok(jwtAuthenticationDto);
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate JWT token", description = "Verifies the validity of a JWT token")
    public ResponseEntity<?> validate(@RequestHeader("Authorization") String authHeader) {
        Boolean validate = userService.validateUserToken(authHeader);
        return ResponseEntity.ok(validate);
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Creates a new user in the system")
    public ResponseEntity<?> createUser(@RequestBody @Valid UserDto userDto) {
        userService.addUser(userDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequestDto req) {
        passwordResetService.sendResetLink(req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequestDto req) {
        passwordResetService.resetPassword(req);
        return ResponseEntity.ok().build();
    }
}