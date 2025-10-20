package renatius.authenticationservice.dto;

import lombok.Data;

@Data
public class JWTAuthenticationDto {
    private String token;
    private String refreshToken;
}
