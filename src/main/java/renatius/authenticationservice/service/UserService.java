package renatius.authenticationservice.service;

import renatius.authenticationservice.dto.*;

public interface UserService {
    JWTAuthenticationDto singIn(UserCredentialsDto userCredentialsDto);
    JWTAuthenticationDto refreshToken(RefreshTokenDto refreshTokenDto);
    boolean addUser(UserDto user);
    boolean validateUserToken(String authHeader);
}
