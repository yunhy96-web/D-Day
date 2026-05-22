package com.flasharena.auth.presentation;

import com.flasharena.auth.application.AuthService;
import com.flasharena.auth.presentation.dto.LoginRequest;
import com.flasharena.auth.presentation.dto.LoginResponse;
import com.flasharena.auth.presentation.dto.MeResponse;
import com.flasharena.global.context.UserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 로그인 — 인터셉터 제외 경로. 성공 시 JWT 발급. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** 보호 프로브 — 유효한 Bearer 토큰이 있어야만 도달. UserContext 에서 사용자 정보를 읽는다. */
    @GetMapping("/me")
    public MeResponse me() {
        return new MeResponse(UserContext.getUserId(), UserContext.getRole());
    }
}
