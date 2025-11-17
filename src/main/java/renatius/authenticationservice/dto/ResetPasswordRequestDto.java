package renatius.authenticationservice.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ResetPasswordRequestDto {

    @NotBlank
    private String token;
    @NotBlank
    private String password;

}
