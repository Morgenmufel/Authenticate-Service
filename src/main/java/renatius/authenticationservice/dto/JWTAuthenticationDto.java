package renatius.authenticationservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "JWT authentication response containing access and refresh tokens")
public class JWTAuthenticationDto {
    @NotBlank(message = "Access token cannot be blank or null")
    private String token;

    @NotBlank(message = "Refresh token cannot be blank or null")
    private String refreshToken;
}
