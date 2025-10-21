package renatius.authenticationservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "DTO for sending refresh token to generate new access token")
public class RefreshTokenDto {
    @NotBlank(message = "Refresh token cannot be null or blank")
    private String refreshToken;
}
