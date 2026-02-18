package renatius.authenticationservice.service;

import renatius.authenticationservice.dto.ForgotPasswordRequestDto;
import renatius.authenticationservice.dto.ResetPasswordRequestDto;

public interface PasswordResetService {
    void resetPassword(ResetPasswordRequestDto req);
    void sendResetLink(ForgotPasswordRequestDto req);
}
