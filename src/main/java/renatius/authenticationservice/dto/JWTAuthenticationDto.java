package renatius.authenticationservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JWTAuthenticationDto {
    @NotBlank(message = "Access token cannot be blank or null")
    private String token;

    @NotBlank(message = "Refresh token cannot be blank or null")
    private String refreshToken;
}
