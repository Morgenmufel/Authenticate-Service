package renatius.authenticationservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import renatius.authenticationservice.exceptions.InvalidTokenException;

import java.io.IOException;
import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JWTFilterTest {

    @Mock
    JWTService jwtService;

    @Mock
    CustomUserServiceImpl customUserService;

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;

    @Mock
    FilterChain filterChain;

    @InjectMocks
    JwtFilter jwtFilter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSkipFilter_WhenNoAuthorizationHeader() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldSkipWhitelistPaths() throws IOException, ServletException {
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer sometoken");
        when(request.getRequestURI())
                .thenReturn("/auth/login");

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
        verifyNoInteractions(customUserService);
    }

    @Test
    void shouldThrow_WhenHeaderNotBearer() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Token AAA");

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(customUserService);
    }

    @Test
    void shouldThrow_WhenTokenInvalid() {
        when(request.getHeader("Authorization")).thenReturn("Bearer badtoken");
        when(request.getRequestURI()).thenReturn("/api/data");
        when(jwtService.validateJwtToken("badtoken")).thenReturn(false);

        assertThatThrownBy(() ->
                jwtFilter.doFilterInternal(request, response, filterChain)
        ).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void shouldAuthenticate_WhenTokenValid() throws ServletException, IOException {
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer goodtoken");
        when(request.getRequestURI())
                .thenReturn("/secured/data");

        when(jwtService.validateJwtToken("goodtoken"))
                .thenReturn(true);

        when(jwtService.getEmailFromToken("goodtoken"))
                .thenReturn("user@mail.com");

        UserDetails user = new User(
                "user@mail.com",
                "password",
                Collections.emptyList()
        );

        when(customUserService.loadUserByUsername("user@mail.com"))
                .thenReturn(user);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(user);

        verify(filterChain).doFilter(request, response);
    }
}
