package renatius.authenticationservice.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import renatius.authenticationservice.dto.ForgotPasswordRequestDto;
import renatius.authenticationservice.dto.ResetPasswordRequestDto;
import renatius.authenticationservice.entity.PasswordResetToken;
import renatius.authenticationservice.entity.User;
import renatius.authenticationservice.exceptions.InvalidTokenException;
import renatius.authenticationservice.exceptions.UserNotFoundException;
import renatius.authenticationservice.repository.PasswordResetTokenRepository;
import renatius.authenticationservice.repository.UserRepository;
import renatius.authenticationservice.service.EmailService;
import renatius.authenticationservice.service.PasswordResetService;
import renatius.authenticationservice.utils.PasswordUtil;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    @Value("${FRONT_URL}")
    private String frontEndpoint;
    private final PasswordResetTokenRepository tokenRepo;
    private final UserRepository userRepo;
    private final EmailService emailService;

    @Override
    @Transactional
    public void sendResetLink(ForgotPasswordRequestDto req) {
        User user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User with "+ req.getEmail() + " not found"));
        tokenRepo.deleteByEmail(user.getEmail());
        String token = UUID.randomUUID().toString();
        PasswordResetToken prt = PasswordResetToken.builder()
                .email(user.getEmail())
                .token(token)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
        tokenRepo.save(prt);
        String link = frontEndpoint + "/reset-password?token=" + token;
        emailService.send(
                user.getEmail(),
                "Password Reset Request",
                "To reset your password," +
                        " click the link:\n" + link + "\n\nThis" +
                        " link expires in 30 minutes."
        );
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDto req) {
        PasswordResetToken token = tokenRepo.findByToken(req.getToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid token"));
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Token expired");}
        User user = userRepo.findByEmail(token.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setPassword(PasswordUtil.encodePassword(req.getPassword()));
        userRepo.save(user);
        tokenRepo.delete(token);
    }
}
