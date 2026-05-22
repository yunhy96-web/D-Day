package com.flasharena.auth.application;

import com.flasharena.auth.domain.User;
import com.flasharena.auth.infrastructure.UserRepository;
import com.flasharena.auth.presentation.dto.LoginRequest;
import com.flasharena.auth.presentation.dto.LoginResponse;
import com.flasharena.global.jwt.JwtProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    /** username 조회 → BCrypt 매칭 → JWT 발급. 실패 시 InvalidCredentialsException(401). */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new InvalidCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        String token = jwtProvider.generateToken(user.getId(), user.getRole());
        return new LoginResponse(token, "Bearer", user.getId(), user.getRole(),
                jwtProvider.getExpirationSeconds());
    }

    /** 인증 실패 예외. 핸들러에서 401 로 매핑. */
    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) {
            super(message);
        }
    }
}
