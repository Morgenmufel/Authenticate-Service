package renatius.authenticationservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AccessTokenDto {
    @NotBlank(message = "access token cannot be blank or null")
    private String accessToken;
}
