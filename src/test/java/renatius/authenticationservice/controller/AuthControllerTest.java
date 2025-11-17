package renatius.authenticationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import renatius.authenticationservice.dto.JWTAuthenticationDto;
import renatius.authenticationservice.exceptions.InvalidCredentialsException;
import renatius.authenticationservice.exceptions.InvalidTokenException;
import renatius.authenticationservice.security.JwtFilter;
import renatius.authenticationservice.service.PasswordResetService;
import renatius.authenticationservice.service.UserService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @Test
    void loginShouldReturnJWT() throws Exception {
        JWTAuthenticationDto jwtAuthenticationDto = new JWTAuthenticationDto();
        jwtAuthenticationDto.setToken("access_token");
        jwtAuthenticationDto.setRefreshToken("refreshToken");
        when(userService.singIn(any())).thenReturn(jwtAuthenticationDto);
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "email@test.com",
                                "password": "123456789"
                            }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("access_token"))
                .andExpect(jsonPath("$.refreshToken").value("refreshToken"));
    }

    @Test
    void loginShouldReturnBadRequestWhenBodyIsInvalid() throws Exception {
        String invalidBody = """
        {
            "email": "",
            "password": ""
        }
        """;
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginShouldReturnUnauthorizedWhenCredentialsAreInvalid() throws Exception {
        when(userService.singIn(any()))
                .thenThrow(new InvalidCredentialsException("Invalid credentials"));
        String body = """
        {
            "email": "email@test.com",
            "password": "123456789"
        }
        """;
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void registerShouldReturnBadRequestWhenBodyIsInvalid() throws Exception {
        String body = """
        {
          "email": "",
          "password": "",
          "username": ""
        }
        """;
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerShouldReturnBadRequestWhenPasswordIsTooShort() throws Exception {
        String body = """
        {
          "email": "valid@test.com",
          "password": "12",         
          "username": "user"
        }
        """;
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerShouldReturnBadRequestWhenEmailIsInvalid() throws Exception {

        String body = """
        {
          "email": "not-an-email",
          "password": "StrongPass123",
          "username": "user"
        }
        """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validateShouldReturnOkWhenTokenValid() throws Exception {

        when(userService.validateUserToken("Bearer valid")).thenReturn(true);

        mockMvc.perform(post("/auth/validate")
                        .header("Authorization", "Bearer valid"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void validateShouldReturnBadRequestWhenTokenBlank() throws Exception {
        mockMvc.perform(post("/auth/validate"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validateShouldReturnUnauthorizedWhenTokenInvalid() throws Exception {
        doThrow(new InvalidTokenException("expired"))
                .when(userService).validateUserToken("Bearer expiredToken");
        mockMvc.perform(post("/auth/validate")
                        .header("Authorization", "Bearer expiredToken"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validateShouldReturnUnauthorizedWhenTokenExpired() throws Exception {
        doThrow(new InvalidTokenException("expired"))
                .when(userService).validateUserToken(anyString());
        mockMvc.perform(post("/auth/validate")
                        .header("Authorization", "Bearer expiredToken"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenShouldReturnNewTokensWhenValid() throws Exception {
        JWTAuthenticationDto response = new JWTAuthenticationDto();
        response.setToken("new-access-token");
        response.setRefreshToken("new-refresh-token");

        when(userService.refreshToken(any())).thenReturn(response);

        mockMvc.perform(post("/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"refreshToken": "valid-refresh-token"}
            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    void refreshTokenShouldReturnBadRequestWhenTokenBlank() throws Exception {

        mockMvc.perform(post("/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"refreshToken": ""}
            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshTokenShouldReturnUnauthorizedWhenTokenInvalid() throws Exception {

        doThrow(new InvalidTokenException("refresh invalid"))
                .when(userService).refreshToken(any());

        mockMvc.perform(post("/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"refreshToken": "bad-token"}
            """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenShouldReturnUnauthorizedWhenTokenExpired() throws Exception {

        doThrow(new InvalidTokenException("expired"))
                .when(userService).refreshToken(any());

        mockMvc.perform(post("/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"refreshToken": "expired-token"}
            """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forgotPassword_ok() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"email": "test@example.com"}
            """))
                .andExpect(status().isOk());

        verify(passwordResetService).sendResetLink(any());
    }

    @Test
    void forgotPassword_invalidEmail() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"email": "bad-email"}
            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_ok() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"token": "abc123", "password": "Pass123!"}
            """))
                .andExpect(status().isOk());

        verify(passwordResetService).resetPassword(any());
    }

    @Test
    void resetPassword_blankFields() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"token": "", "password": ""}
            """))
                .andExpect(status().isBadRequest());
    }

}
