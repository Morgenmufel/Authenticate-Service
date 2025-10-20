package renatius.authenticationservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenDto {
    @NotBlank(message = "Refresh token cannot be null or blank")
    private String refreshToken;
}
