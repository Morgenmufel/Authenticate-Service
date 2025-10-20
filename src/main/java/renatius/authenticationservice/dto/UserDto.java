package renatius.authenticationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserDto {

    @NotBlank(message = "Username cannot be blank or empty")
    @Size(min = 5, message = "Username must contains at once 5 symbols")
    private String username;

    @Email(message = "Email address has invalid format: ${validatedValue}",
            regexp = "^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$")
    private String email;

    @NotBlank(message = "Password cannot be null or blank")
    @Size(min = 8, message = "Password must contains at once 8 symbols")
    private String password;
}