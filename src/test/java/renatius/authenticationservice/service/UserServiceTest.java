package renatius.authenticationservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import renatius.authenticationservice.dto.*;
import renatius.authenticationservice.entity.User;
import renatius.authenticationservice.exceptions.InvalidCredentialsException;
import renatius.authenticationservice.exceptions.InvalidTokenException;
import renatius.authenticationservice.exceptions.UserAlreadyExistsException;
import renatius.authenticationservice.exceptions.UserNotFoundException;
import renatius.authenticationservice.mapper.UserMapper;
import renatius.authenticationservice.repository.UserRepository;
import renatius.authenticationservice.security.JWTService;
import renatius.authenticationservice.service.impl.UserServiceImpl;
import renatius.authenticationservice.utils.PasswordUtil;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JWTService jwtService;

    @InjectMocks
    private UserServiceImpl service;

    @Test
    void signIn_shouldReturnTokens_WhenCredentialsCorrect() {
        UserCredentialsDto dto = UserCredentialsDto.builder()
                .email("user@mail.com")
                .password("password123")
                .build();

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@mail.com");
        user.setUsername("username");
        user.setPassword(PasswordUtil.encodePassword("password123"));

        when(userRepository.findByEmail("user@mail.com")).thenReturn(Optional.of(user));

        JWTAuthenticationDto jwtResp = new JWTAuthenticationDto();
        jwtResp.setToken("tokenA");
        jwtResp.setRefreshToken("refreshA");
        when(jwtService.generateAuthToken(user.getId(), user.getUsername(), user.getEmail()))
                .thenReturn(jwtResp);

        JWTAuthenticationDto result = service.singIn(dto);

        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("tokenA");
        assertThat(result.getRefreshToken()).isEqualTo("refreshA");
    }

    @Test
    void signIn_shouldThrowInvalidCredentials_WhenPasswordWrong() {
        UserCredentialsDto dto = UserCredentialsDto.builder()
                .email("user@mail.com")
                .password("wrong")
                .build();

        User user = new User();
        user.setEmail("user@mail.com");
        user.setPassword(PasswordUtil.encodePassword("correct"));

        when(userRepository.findByEmail("user@mail.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.singIn(dto))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void signIn_shouldThrowUserNotFound_WhenEmailNotExist() {
        when(userRepository.findByEmail("notfound@mail.com")).thenReturn(Optional.empty());

        UserCredentialsDto dto = UserCredentialsDto.builder()
                .email("notfound@mail.com")
                .password("password123")
                .build();

        assertThatThrownBy(() -> service.singIn(dto))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void refreshToken_shouldReturnNewTokens_WhenValid() {
        RefreshTokenDto dto = RefreshTokenDto.builder()
                .refreshToken("refresh123")
                .build();

        when(jwtService.validateJwtToken("refresh123")).thenReturn(true);
        when(jwtService.getEmailFromToken("refresh123")).thenReturn("user@mail.com");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@mail.com");
        user.setUsername("username");

        when(userRepository.findByEmail("user@mail.com")).thenReturn(Optional.of(user));

        JWTAuthenticationDto resp = new JWTAuthenticationDto();
        resp.setToken("newToken");
        resp.setRefreshToken("newRefresh");

        when(jwtService.refreshBaseToken(user.getId(), user.getUsername(), user.getEmail(), "refresh123"))
                .thenReturn(resp);

        JWTAuthenticationDto result = service.refreshToken(dto);

        assertThat(result.getToken()).isEqualTo("newToken");
        assertThat(result.getRefreshToken()).isEqualTo("newRefresh");
    }

    @Test
    void refreshToken_shouldThrow_WhenInvalid() {
        RefreshTokenDto dto = RefreshTokenDto.builder()
                .refreshToken("badToken")
                .build();

        when(jwtService.validateJwtToken("badToken")).thenReturn(false);

        assertThatThrownBy(() -> service.refreshToken(dto))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void addUser_shouldCreateUser_WhenValid() {
        UserDto dto = UserDto.builder()
                .username("validName")
                .email("email@mail.com")
                .password("password123")
                .build();

        when(userRepository.existsByEmail("email@mail.com")).thenReturn(false);
        when(userRepository.existsByUsername("validName")).thenReturn(false);

        User mapped = new User();
        mapped.setUsername("validName");
        mapped.setEmail("email@mail.com");
        mapped.setPassword("password123");

        when(userMapper.toEntity(dto)).thenReturn(mapped);

        boolean result = service.addUser(dto);

        assertThat(result).isTrue();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void addUser_shouldThrow_WhenEmailExists() {
        UserDto dto = UserDto.builder()
                .username("validName")
                .email("email@mail.com")
                .password("password123")
                .build();

        when(userRepository.existsByEmail("email@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> service.addUser(dto))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void validateUserToken_shouldReturnTrue_WhenTokenValid() {
        when(jwtService.validateJwtToken("abc")).thenReturn(true);

        boolean result = service.validateUserToken("Bearer abc");

        assertThat(result).isTrue();
    }

    @Test
    void validateUserToken_shouldThrow_WhenHeaderMissingBearer() {
        assertThatThrownBy(() -> service.validateUserToken("abc"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void validateUserToken_shouldThrow_WhenJwtInvalid() {
        when(jwtService.validateJwtToken("bad")).thenReturn(false);

        assertThatThrownBy(() -> service.validateUserToken("Bearer bad"))
                .isInstanceOf(InvalidTokenException.class);
    }
}
