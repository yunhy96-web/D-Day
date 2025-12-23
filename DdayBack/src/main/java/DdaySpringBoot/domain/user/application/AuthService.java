package DdaySpringBoot.domain.user.application;

import DdaySpringBoot.domain.user.domain.Role;
import DdaySpringBoot.domain.user.domain.User;
import DdaySpringBoot.domain.user.domain.UserRepository;
import DdaySpringBoot.domain.user.dto.LoginRequest;
import DdaySpringBoot.domain.user.dto.SignUpRequest;
import DdaySpringBoot.domain.user.dto.TokenRefreshRequest;
import DdaySpringBoot.domain.user.dto.TokenResponse;
import DdaySpringBoot.global.config.jwt.JwtTokenProvider;
import DdaySpringBoot.global.exception.AuthException;
import DdaySpringBoot.global.exception.BusinessException;
import DdaySpringBoot.global.exception.EntityNotFoundException;
import DdaySpringBoot.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService implements AuthServiceInterface {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.access-token-validity}")
    private long accessTokenValidity;

    @Override
    @Transactional
    public Long signUp(SignUpRequest request) {
        validateDuplicateEmail(request.getEmail());
        validateTimezone(request.getTimezone());

        User user = User.builder()
                .email(request.getEmail())
                .nickname(request.getNickname())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .timezone(request.getTimezone())
                .build();

        return userRepository.save(user).getNo();
    }

    private void validateTimezone(String timezone) {
        try {
            java.time.ZoneId.of(timezone);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지 않은 타임존입니다: " + timezone);
        }
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = findUserByEmail(request.getEmail());

        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

        user.updateRefreshToken(refreshToken);

        return TokenResponse.of(accessToken, refreshToken, accessTokenValidity / 1000, user.getTimezone(), user.getNickname(), user.getNo(), user.getRole().name());
    }

    @Override
    @Transactional
    public TokenResponse refreshToken(TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new AuthException(ErrorCode.INVALID_TOKEN);
        }

        String email = jwtTokenProvider.getEmail(refreshToken);
        User user = findUserByEmail(email);

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new AuthException(ErrorCode.INVALID_TOKEN, "Refresh token이 일치하지 않습니다");
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(email);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(email);

        user.updateRefreshToken(newRefreshToken);

        return TokenResponse.of(newAccessToken, newRefreshToken, accessTokenValidity / 1000, user.getTimezone(), user.getNickname(), user.getNo(), user.getRole().name());
    }

    @Override
    @Transactional
    public void logout(String email) {
        User user = findUserByEmail(email);
        user.updateRefreshToken(null);
    }

    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));
    }
}
