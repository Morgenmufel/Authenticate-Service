package renatius.authenticationservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import renatius.authenticationservice.dto.JWTAuthenticationDto;
import renatius.authenticationservice.dto.RefreshTokenDto;
import renatius.authenticationservice.dto.UserCredentialsDto;
import renatius.authenticationservice.dto.UserDto;
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

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JWTService jwtService;
    private final PasswordUtil passwordUtil;

    @Override
    public JWTAuthenticationDto singIn(UserCredentialsDto userCredentialsDto) throws AuthenticationException {
        if (userCredentialsDto == null
                || userCredentialsDto.getEmail() == null
                || userCredentialsDto.getPassword() == null) {
            throw new InvalidCredentialsException("Email or password cannot be null");
        }
        User user = findByCredentials(userCredentialsDto);



        return jwtService.generateAuthToken(user.getId(),user.getUsername(), user.getEmail());
    }

    @Override
    public JWTAuthenticationDto refreshToken(RefreshTokenDto refreshTokenDto) throws Exception {
        if (refreshTokenDto == null || refreshTokenDto.getRefreshToken() == null) {
            throw new InvalidTokenException("Refresh token is missing");
        }
        String refreshToken = refreshTokenDto.getRefreshToken().trim();
        if (refreshToken != null && jwtService.validateJwtToken(refreshToken)) {
            User user = findByEmail(jwtService.getEmailFromToken(refreshToken));
            return jwtService.refreshBaseToken(user.getId(),user.getUsername(), user.getEmail(), refreshToken);
        } else {
          throw new InvalidTokenException("Refresh token is invalid");
        }
    }

//    @Override
//    @Transactional
//    public UserDto getUserById(String id) throws ChangeSetPersister.NotFoundException {
//        return userMapper.toDto(userRepository.findById(UUID.fromString(id))
//                .orElseThrow(ChangeSetPersister.NotFoundException::new));
//    }
//
//    @Override
//    @Transactional
//    public UserDto getUserByEmail(String email) throws ChangeSetPersister.NotFoundException {
//        return userMapper.toDto(userRepository.findByEmail(email)
//                .orElseThrow(ChangeSetPersister.NotFoundException::new));
//    }

    @Override
    public boolean addUser(UserDto userDto){
        if (userRepository.findByEmail(userDto.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("User with email " + userDto.getEmail() + " already exists");
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
    public boolean validateUserToken(String token) throws AuthenticationException {
        if (token == null || token.isEmpty()) {
            throw new InvalidTokenException("Token cannot be null or empty");
        }
        if (!jwtService.validateJwtToken(token)) {
            throw new InvalidTokenException("Token is invalid or expired");
        }
        return true;
    }

    private User findByCredentials(UserCredentialsDto userCredentialsDto)  {
        Optional<User> optionalUser = Optional.ofNullable(userRepository.findByEmail(userCredentialsDto.getEmail()).orElseThrow(
                () -> new UserNotFoundException("User with this credentials not found")));
        if (optionalUser.isPresent()){
            User user = optionalUser.get();
            if (PasswordUtil.validatePassword(userCredentialsDto.getPassword(), user.getPassword())){
                return user;
            }
        }
        return null;
    }

    private User findByEmail(String email) throws Exception {
        return userRepository.findByEmail(email)
                .orElseThrow(()-> new UserNotFoundException("User with this email not found"));
    }
}