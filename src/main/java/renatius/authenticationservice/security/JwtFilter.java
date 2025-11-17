package renatius.authenticationservice.security;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import renatius.authenticationservice.exceptions.InvalidTokenException;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LogManager.getLogger(JwtFilter.class);
    private final JWTService jwtService;
    private final CustomUserServiceImpl customUserService;
    private static final String[] WHITELIST = {
            "/auth/register",
            "/auth/login",
            "/auth/refresh-token",
            "/auth/reset-password",
            "/auth/forgot-password"
    };

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String path = request.getRequestURI();
        for (String open : WHITELIST) {
            if (path.startsWith(open)) {
                filterChain.doFilter(request, response);
                return;
            }
        }
        String token = getTokenFromRequest(request);
        if (!jwtService.validateJwtToken(token)) {
            throw new InvalidTokenException("Invalid JWT token");
        }
        setCustomUserDetailsToSecurityContextHolder(token);
        filterChain.doFilter(request, response);
    }

    private void setCustomUserDetailsToSecurityContextHolder(String token) {
        String email = jwtService.getEmailFromToken(token);
        UserDetails customUserDetails = customUserService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(customUserDetails,
                null, customUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearerToken == null || bearerToken.isBlank()) {
            LOGGER.error("JWT token not found in Authorization header");
            throw new InvalidTokenException("JWT token not found in Authorization header or wrong Header");
        }
        if (bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7).trim();
        }
        throw new InvalidTokenException("Invalid JWT token: The token must start with 'Bearer '");
    }
}
