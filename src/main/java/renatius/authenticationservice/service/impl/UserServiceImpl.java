package renatius.authenticationservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import renatius.authenticationservice.dto.UserDto;
import renatius.authenticationservice.dto.RefreshTokenDto;
import renatius.authenticationservice.dto.JWTAuthenticationDto;
import renatius.authenticationservice.dto.UserCredentialsDto;
import renatius.authenticationservice.entity.User;
import renatius.authenticationservice.exceptions.InvalidCredentialsException;
import renatius.authenticationservice.exceptions.InvalidTokenException;
import renatius.authenticationservice.exceptions.UserAlreadyExistsException;
import renatius.authenticationservice.exceptions.UserNotFoundException;
import renatius.authenticationservice.mapper.UserMapper;
import renatius.authenticationservice.repository.UserRepository;
import renatius.authenticationservice.security.JWTService;
import renatius.authenticationservice.service.UserService;
import renatius.authenticationservice.utils.PasswordUtil;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JWTService jwtService;
    private static final Logger LOGGER = LogManager.getLogger(UserServiceImpl.class);

    @Override
    public JWTAuthenticationDto singIn(UserCredentialsDto userCredentialsDto) {
        User user = findByCredentials(userCredentialsDto);
        return jwtService.generateAuthToken(user.getId(),user.getUsername(), user.getEmail());
    }

    @Override
    public JWTAuthenticationDto refreshToken(RefreshTokenDto refreshTokenDto)  {
        String refreshToken = refreshTokenDto.getRefreshToken().trim();
        if (!jwtService.validateJwtToken(refreshToken)) {
            LOGGER.error("Refresh token is invalid");
            throw new InvalidTokenException("Refresh token is invalid");
        }
        User user = findByEmail(jwtService.getEmailFromToken(refreshToken));
        return jwtService.refreshBaseToken(user.getId(),user.getUsername(), user.getEmail(), refreshToken);
    }

    @Override
    public boolean addUser(UserDto userDto){
        if (userRepository.existsByEmail(userDto.getEmail())
                || userRepository.existsByUsername(userDto.getUsername())) {
            LOGGER.error("User already exists");
            throw new UserAlreadyExistsException("User already exists");
        }
        User user = userMapper.toEntity(userDto);
        UUID id = UUID.randomUUID();
        user.setId(id);
        String hashPassword = PasswordUtil.encodePassword(user.getPassword());
        user.setPassword(hashPassword);
        userRepository.save(user);
        return true;
    }

    @Override
    public boolean validateUserToken(String authHeader) {
        if(authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!jwtService.validateJwtToken(token)) {
                throw new InvalidTokenException("Token is invalid or expired");
            }
            return true;
        }
        LOGGER.error("Invalid token");
        throw new InvalidTokenException("Invalid token");
    }

    private User findByCredentials(UserCredentialsDto userCredentialsDto)  {
        User user = userRepository.findByEmail(userCredentialsDto.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User with this credentials not found"));
            if (PasswordUtil.validatePassword(userCredentialsDto.getPassword(), user.getPassword())){
                return user;
            }
                LOGGER.error("Invalid credentials");
                throw new InvalidCredentialsException("Invalid credentials");
    }

    private User findByEmail(String email)  {
        return userRepository.findByEmail(email)
                .orElseThrow(()-> new UserNotFoundException("User with this email not found"));
    }
}