package com.hauly.platform.auth.application;

import com.hauly.platform.auth.application.command.BootstrapUserCommand;
import com.hauly.platform.auth.application.command.LoginCommand;
import com.hauly.platform.auth.application.command.RefreshCommand;
import com.hauly.platform.auth.application.query.CurrentUserView;
import com.hauly.platform.auth.domain.event.UserAuthenticatedEvent;
import com.hauly.platform.auth.domain.model.AppUser;
import com.hauly.platform.auth.domain.model.Email;
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
     * Throws BadCredentialsException on failure (email not found or wrong password).
     */
    @Transactional(readOnly = true)
    public LoginResult login(LoginCommand command) {
        Email email;
        try {
            email = Email.of(command.email());
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid credentials");
        }

        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        boolean matches = user.verifyPassword(command.password(),
                (raw, hash) -> passwordEncoder.matches(raw, hash));
        if (!matches) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String accessToken = tokenService.issueAccessToken(user.getId(), user.getRole().name());
        String refreshToken = tokenService.issueRefreshToken(user.getId(), user.getRole().name());

        eventPublisher.publishEvent(
                UserAuthenticatedEvent.of(user.getId(), user.getEmailValue(), user.getRole().name()));

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
     * Create a bootstrap user (used by InitialUserBootstrapper).
     */
    public AppUser bootstrapUser(BootstrapUserCommand command) {
        passwordPolicyService.validate(command.rawPassword());
        Email email = Email.of(command.email());
        AppUser user = AppUser.create(
                email,
                command.rawPassword(),
                command.role(),
                command.displayName(),
                rawPwd -> passwordEncoder.encode(rawPwd)
        );
        return userRepository.save(user);
    }

    /**
     * Result record for login use case.
     */
    public record LoginResult(String accessToken, String refreshToken, CurrentUserView user) {}
}
