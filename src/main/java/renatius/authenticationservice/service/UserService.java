package renatius.authenticationservice.service;

import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.security.core.AuthenticationException;
import renatius.authenticationservice.dto.*;

public interface UserService {

    JWTAuthenticationDto singIn(UserCredentialsDto userCredentialsDto);
    JWTAuthenticationDto refreshToken(RefreshTokenDto refreshTokenDto);
    boolean addUser(UserDto user);
    boolean validateUserToken(String authHeader);
}
