package renatius.authenticationservice.service;

import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.security.core.AuthenticationException;
import renatius.authenticationservice.dto.JWTAuthenticationDto;
import renatius.authenticationservice.dto.RefreshTokenDto;
import renatius.authenticationservice.dto.UserCredentialsDto;
import renatius.authenticationservice.dto.UserDto;

public interface UserService {

    JWTAuthenticationDto singIn(UserCredentialsDto userCredentialsDto) throws AuthenticationException;
    JWTAuthenticationDto refreshToken(RefreshTokenDto refreshTokenDto) throws Exception;
    //UserDto getUserById(String id) throws ChangeSetPersister.NotFoundException;
    //UserDto getUserByEmail(String email) throws ChangeSetPersister.NotFoundException;
    boolean addUser(UserDto user);
    boolean validateUserToken(String token) throws AuthenticationException;
}
