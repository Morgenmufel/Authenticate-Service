package renatius.authenticationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import renatius.authenticationservice.dto.AccessTokenDto;
import renatius.authenticationservice.dto.JWTAuthenticationDto;
import renatius.authenticationservice.dto.UserCredentialsDto;
import renatius.authenticationservice.dto.UserDto;
import renatius.authenticationservice.dto.RefreshTokenDto;
import renatius.authenticationservice.service.UserService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<JWTAuthenticationDto> singIn(@RequestBody @Valid UserCredentialsDto userCredentialsDto) {
        return ResponseEntity.ok(userService.singIn(userCredentialsDto));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<JWTAuthenticationDto> refresh(@RequestBody @Valid RefreshTokenDto refreshTokenDto) {
       JWTAuthenticationDto jwtAuthenticationDto = userService.refreshToken(refreshTokenDto);
       return ResponseEntity.ok(jwtAuthenticationDto);
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validate(@RequestHeader("Authorization") String authHeader) {
        Boolean validate = userService.validateUserToken(authHeader);
        return ResponseEntity.ok(validate);
    }

    @PostMapping("/register")
    public ResponseEntity<?> createUser(@RequestBody @Valid UserDto userDto) {
        return ResponseEntity.ok(userService.addUser(userDto));
    }
}

