package DdaySpringBoot.service;

import DdaySpringBoot.dto.auth.LoginRequest;
import DdaySpringBoot.dto.auth.SignUpRequest;
import DdaySpringBoot.dto.auth.TokenRefreshRequest;
import DdaySpringBoot.dto.auth.TokenResponse;

public interface AuthServiceInterface {

    Long signUp(SignUpRequest request);

    TokenResponse login(LoginRequest request);

    TokenResponse refreshToken(TokenRefreshRequest request);

    void logout(String email);
}
