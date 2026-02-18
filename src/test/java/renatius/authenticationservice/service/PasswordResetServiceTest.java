package renatius.authenticationservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import renatius.authenticationservice.dto.ForgotPasswordRequestDto;
import renatius.authenticationservice.dto.ResetPasswordRequestDto;
import renatius.authenticationservice.entity.PasswordResetToken;
import renatius.authenticationservice.entity.User;
import renatius.authenticationservice.exceptions.InvalidTokenException;
import renatius.authenticationservice.exceptions.UserNotFoundException;
import renatius.authenticationservice.repository.PasswordResetTokenRepository;
import renatius.authenticationservice.repository.UserRepository;
import renatius.authenticationservice.service.impl.PasswordResetServiceImpl;
import renatius.authenticationservice.utils.PasswordUtil;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class PasswordResetServiceTest {

    private PasswordResetTokenRepository tokenRepo;
    private UserRepository userRepo;
    private EmailService emailService;
    private PasswordResetServiceImpl service;

    @BeforeEach
    void setUp() {
        tokenRepo = mock(PasswordResetTokenRepository.class);
        userRepo = mock(UserRepository.class);
        emailService = mock(EmailService.class);

        service = new PasswordResetServiceImpl(tokenRepo, userRepo, emailService);
        ReflectionTestUtils.setField(service, "frontEndpoint", "http://localhost:3000");
    }

    @Test
    void sendResetLink_shouldSendEmail_WhenUserExists() {
        ForgotPasswordRequestDto req = ForgotPasswordRequestDto.builder()
                .email("test@mail.com")
                .build();

        User user = new User();
        user.setEmail("test@mail.com");

        when(userRepo.findByEmail("test@mail.com")).thenReturn(Optional.of(user));

        service.sendResetLink(req);
        verify(tokenRepo).deleteByEmail("test@mail.com");
        ArgumentCaptor<PasswordResetToken> tokenCaptor =
                ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepo).save(tokenCaptor.capture());
        PasswordResetToken saved = tokenCaptor.getValue();

        assertThat(saved.getEmail()).isEqualTo("test@mail.com");
        assertThat(saved.getToken()).isNotBlank();
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
        verify(emailService).send(
                eq("test@mail.com"),
                eq("Password Reset Request"),
                contains("reset-password?token=")
        );
    }

    @Test
    void sendResetLink_shouldThrow_WhenUserNotFound() {
        ForgotPasswordRequestDto req = ForgotPasswordRequestDto.builder()
                .email("unknown@mail.com")
                .build();

        when(userRepo.findByEmail("unknown@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendResetLink(req))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void resetPassword_shouldUpdatePassword_WhenTokenValid() {
        ResetPasswordRequestDto req = ResetPasswordRequestDto.builder()
                .token("abc")
                .password("newPass")
                .build();
        PasswordResetToken prt = PasswordResetToken.builder()
                .token("abc")
                .email("user@mail.com")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        User user = new User();
        user.setEmail("user@mail.com");
        user.setPassword("old");
        when(tokenRepo.findByToken("abc")).thenReturn(Optional.of(prt));
        when(userRepo.findByEmail("user@mail.com")).thenReturn(Optional.of(user));
        service.resetPassword(req);
        assertThat(user.getPassword()).isNotEqualTo("old");
        assertThat(PasswordUtil.validatePassword("newPass", user.getPassword())).isTrue();
        verify(userRepo).save(user);
        verify(tokenRepo).delete(prt);
    }

    @Test
    void resetPassword_shouldThrow_WhenTokenNotFound() {
        when(tokenRepo.findByToken("abc")).thenReturn(Optional.empty());

        ResetPasswordRequestDto req = ResetPasswordRequestDto.builder()
                .token("abc")
                .password("pass")
                .build();

        assertThatThrownBy(() -> service.resetPassword(req))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void resetPassword_shouldThrow_WhenTokenExpired() {
        PasswordResetToken prt = PasswordResetToken.builder()
                .token("abc")
                .email("user@mail.com")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(tokenRepo.findByToken("abc")).thenReturn(Optional.of(prt));

        ResetPasswordRequestDto req = ResetPasswordRequestDto.builder()
                .token("abc")
                .password("pass")
                .build();

        assertThatThrownBy(() -> service.resetPassword(req))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void resetPassword_shouldThrow_WhenUserNotFound() {
        PasswordResetToken prt = PasswordResetToken.builder()
                .token("abc")
                .email("user@mail.com")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(tokenRepo.findByToken("abc")).thenReturn(Optional.of(prt));
        when(userRepo.findByEmail("user@mail.com")).thenReturn(Optional.empty());

        ResetPasswordRequestDto req = ResetPasswordRequestDto.builder()
                .token("abc")
                .password("pass")
                .build();

        assertThatThrownBy(() -> service.resetPassword(req))
                .isInstanceOf(UserNotFoundException.class);
    }
}

