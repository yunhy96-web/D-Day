package com.hauly.platform.auth.application;

import com.hauly.platform.auth.application.command.BootstrapUserCommand;
import com.hauly.platform.auth.application.command.ChangePasswordCommand;
import com.hauly.platform.auth.application.command.LoginCommand;
import com.hauly.platform.auth.application.command.RefreshCommand;
import com.hauly.platform.auth.application.query.CurrentUserView;
import com.hauly.platform.auth.domain.event.UserAuthenticatedEvent;
import com.hauly.platform.auth.domain.model.AppUser;
import com.hauly.platform.auth.domain.model.Username;
import com.hauly.platform.auth.domain.repository.AppUserRepository;
import com.hauly.platform.auth.domain.service.PasswordPolicyService;
import com.hauly.platform.auth.domain.service.TokenService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service orchestrating auth use cases.
 * Calls domain and infrastructure; publishes domain events.
 */
@Service
@Transactional
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordPolicyService passwordPolicyService;

    public AuthService(AppUserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.eventPublisher = eventPublisher;
        this.passwordPolicyService = new PasswordPolicyService();
    }

    /**
     * Authenticate a user and return tokens.
     * Throws BadCredentialsException on failure (username not found or wrong password).
     */
    @Transactional(readOnly = true)
    public LoginResult login(LoginCommand command) {
        Username username;
        try {
            username = Username.of(command.username());
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid credentials");
        }

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        boolean matches = user.verifyPassword(command.password(),
                (raw, hash) -> passwordEncoder.matches(raw, hash));
        if (!matches) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String accessToken = tokenService.issueAccessToken(user.getId(), user.getRole().name());
        String refreshToken = tokenService.issueRefreshToken(user.getId(), user.getRole().name());

        eventPublisher.publishEvent(
                UserAuthenticatedEvent.of(user.getId(), user.getUsernameValue(), user.getRole().name()));

        return new LoginResult(accessToken, refreshToken, CurrentUserView.from(user));
    }

    /**
     * Refresh access token from a valid refresh token.
     */
    @Transactional(readOnly = true)
    public String refresh(RefreshCommand command) {
        TokenService.TokenClaims claims = tokenService.validateRefreshToken(command.refreshToken());
        return tokenService.issueAccessToken(claims.userId(), claims.role());
    }

    /**
     * Look up the current user by ID.
     */
    @Transactional(readOnly = true)
    public CurrentUserView currentUser(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        return CurrentUserView.from(user);
    }

    /**
     * Change password for the currently authenticated user.
     * Verifies the current password, validates the new one against policy, then persists.
     */
    public void changePassword(ChangePasswordCommand command) {
        AppUser user = userRepository.findById(command.userId())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        boolean matches = user.verifyPassword(command.currentPassword(),
                (raw, hash) -> passwordEncoder.matches(raw, hash));
        if (!matches) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        passwordPolicyService.validate(command.newPassword());
        user.changePassword(command.newPassword(), passwordEncoder::encode);
        userRepository.save(user);
    }

    /**
     * Create a bootstrap user (used by InitialUserBootstrapper).
     */
    public AppUser bootstrapUser(BootstrapUserCommand command) {
        passwordPolicyService.validate(command.rawPassword());
        Username username = Username.of(command.username());
        AppUser user = AppUser.create(
                username,
                command.rawPassword(),
                command.role(),
                command.displayName(),
                rawPwd -> passwordEncoder.encode(rawPwd)
        );
        return userRepository.save(user);
    }

    /**
     * Atomic idempotent variant — find-by-username-or-create — for bootstrap callers.
     * The single transaction closes the TOCTOU window between the existence check and
     * the insert, and the unique constraint on username remains the ultimate safety net
     * against a race with a concurrently-starting JVM.
     *
     * Returns a result describing whether the user was newly created (so callers can
     * record the freshly generated password) or already existed (so they skip recording).
     */
    public BootstrapResult ensureUser(BootstrapUserCommand command) {
        Username username = Username.of(command.username());
        return userRepository.findByUsername(username)
                .map(existing -> new BootstrapResult(existing, false))
                .orElseGet(() -> new BootstrapResult(bootstrapUser(command), true));
    }

    public record BootstrapResult(AppUser user, boolean created) {}

    /**
     * Result record for login use case.
     */
    public record LoginResult(String accessToken, String refreshToken, CurrentUserView user) {}
}
