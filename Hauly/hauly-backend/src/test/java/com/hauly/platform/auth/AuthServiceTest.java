package com.hauly.platform.auth;

import com.hauly.platform.auth.application.AuthService;
import com.hauly.platform.auth.application.command.LoginCommand;
import com.hauly.platform.auth.application.command.RefreshCommand;
import com.hauly.platform.auth.application.query.CurrentUserView;
import com.hauly.platform.auth.domain.model.AppUser;
import com.hauly.platform.auth.domain.model.Role;
import com.hauly.platform.auth.domain.model.Username;
import com.hauly.platform.auth.domain.repository.AppUserRepository;
import com.hauly.platform.auth.domain.service.TokenService;
import com.hauly.platform.auth.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String TEST_SECRET = "test-secret-min-32-chars-for-hs256-algorithm-ok!";
    private static final String RAW_PASSWORD = "changeme-12345";
    private static final Long   TEST_USER_ID = 42L;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // BCrypt cost 4 for test speed (real encoding, not mocked)
        passwordEncoder = new BCryptPasswordEncoder(4);
        jwtTokenProvider = new JwtTokenProvider(TEST_SECRET, 30, 14);
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider, eventPublisher);
    }

    @Test
    void login_success() {
        AppUser user = buildUserWithId("intake", RAW_PASSWORD, Role.INTAKE, TEST_USER_ID);
        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.of(user));

        AuthService.LoginResult result = authService.login(new LoginCommand("intake", RAW_PASSWORD));

        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.user().username()).isEqualTo("intake");
        assertThat(result.user().role()).isEqualTo("INTAKE");

        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void login_wrongPassword_throwsBadCredentials() {
        AppUser user = buildUserWithId("intake", RAW_PASSWORD, Role.INTAKE, TEST_USER_ID);
        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginCommand("intake", "wrong-password-12345")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_unknownUsername_throwsBadCredentials() {
        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginCommand("nobody", RAW_PASSWORD)))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_success() {
        // Issue a refresh token directly via provider (no need to login)
        String refreshToken = jwtTokenProvider.issueRefreshToken(TEST_USER_ID, "BUYER");

        String newAccessToken = authService.refresh(new RefreshCommand(refreshToken));

        assertThat(newAccessToken).isNotBlank();
        TokenService.TokenClaims claims = jwtTokenProvider.validateAccessToken(newAccessToken);
        assertThat(claims.userId()).isEqualTo(TEST_USER_ID);
        assertThat(claims.role()).isEqualTo("BUYER");
    }

    @Test
    void currentUser_success() {
        AppUser user = buildUserWithId("admin", RAW_PASSWORD, Role.ADMIN, TEST_USER_ID);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));

        CurrentUserView view = authService.currentUser(TEST_USER_ID);

        assertThat(view.username()).isEqualTo("admin");
        assertThat(view.role()).isEqualTo("ADMIN");
    }

    // -----------------------------------------------------------------------

    /**
     * Creates an AppUser and sets its ID via Mockito spy.
     * JPA would normally set the ID after persist; we simulate it here.
     */
    private AppUser buildUserWithId(String username, String rawPassword, Role role, Long id) {
        AppUser user = AppUser.create(
                Username.of(username),
                rawPassword,
                role,
                "Test User",
                pwd -> passwordEncoder.encode(pwd)
        );
        AppUser spied = spy(user);
        lenient().when(spied.getId()).thenReturn(id);
        return spied;
    }
}
