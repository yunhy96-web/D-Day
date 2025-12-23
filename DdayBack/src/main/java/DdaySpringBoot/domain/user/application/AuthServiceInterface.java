package DdaySpringBoot.domain.user.application;

import DdaySpringBoot.domain.user.dto.LoginRequest;
import DdaySpringBoot.domain.user.dto.SignUpRequest;
import DdaySpringBoot.domain.user.dto.TokenRefreshRequest;
import DdaySpringBoot.domain.user.dto.TokenResponse;

public interface AuthServiceInterface {

    Long signUp(SignUpRequest request);

    TokenResponse login(LoginRequest request);

    TokenResponse refreshToken(TokenRefreshRequest request);

    void logout(String email);
}
