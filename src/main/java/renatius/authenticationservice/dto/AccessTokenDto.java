package renatius.authenticationservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "DTO representing a JWT access token")
public class AccessTokenDto {
    @NotBlank(message = "access token cannot be blank or null")
    private String accessToken;
}
