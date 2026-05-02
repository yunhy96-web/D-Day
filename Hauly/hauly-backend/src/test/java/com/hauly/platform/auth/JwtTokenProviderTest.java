package com.hauly.platform.auth;

import com.hauly.platform.auth.domain.service.TokenService;
import com.hauly.platform.auth.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    // Secret >= 256 bits for HS256 (this is 48 chars = 384 bits)
    private static final String TEST_SECRET = "test-secret-min-32-chars-for-hs256-algorithm-ok!";

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(TEST_SECRET, 30, 14);
    }

    @Test
    void issueAndValidateAccessToken_happyPath() {
        String token = provider.issueAccessToken(42L, "INTAKE");

        TokenService.TokenClaims claims = provider.validateAccessToken(token);

        assertThat(claims.userId()).isEqualTo(42L);
        assertThat(claims.role()).isEqualTo("INTAKE");
    }

    @Test
    void issueAndValidateRefreshToken_happyPath() {
        String token = provider.issueRefreshToken(99L, "BUYER");

        TokenService.TokenClaims claims = provider.validateRefreshToken(token);

        assertThat(claims.userId()).isEqualTo(99L);
        assertThat(claims.role()).isEqualTo("BUYER");
    }

    @Test
    void accessTokenRejectedAsRefreshToken() {
        String token = provider.issueAccessToken(1L, "ADMIN");

        assertThatThrownBy(() -> provider.validateRefreshToken(token))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("not a refresh token");
    }

    @Test
    void refreshTokenRejectedAsAccessToken() {
        String token = provider.issueRefreshToken(1L, "ADMIN");

        assertThatThrownBy(() -> provider.validateAccessToken(token))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("not an access token");
    }

    @Test
    void expiredToken_throwsBadCredentials() {
        // 0-minute TTL → immediately expired
        JwtTokenProvider shortLived = new JwtTokenProvider(TEST_SECRET, 0, 14);
        String token = shortLived.issueAccessToken(1L, "INTAKE");

        assertThatThrownBy(() -> provider.validateAccessToken(token))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void tamperedSignature_throwsBadCredentials() {
        String token = provider.issueAccessToken(1L, "INTAKE");
        // Corrupt the last character of the signature
        String tampered = token.substring(0, token.length() - 3) + "XXX";

        assertThatThrownBy(() -> provider.validateAccessToken(tampered))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void malformedToken_throwsBadCredentials() {
        assertThatThrownBy(() -> provider.validateAccessToken("not.a.jwt.at.all"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void validateAccessToken_null_throwsBadCredentials() {
        assertThatThrownBy(() -> provider.validateAccessToken(null))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void validateAccessToken_empty_throwsBadCredentials() {
        assertThatThrownBy(() -> provider.validateAccessToken(""))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void validateRefreshToken_null_throwsBadCredentials() {
        assertThatThrownBy(() -> provider.validateRefreshToken(null))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void validateRefreshToken_empty_throwsBadCredentials() {
        assertThatThrownBy(() -> provider.validateRefreshToken(""))
                .isInstanceOf(BadCredentialsException.class);
    }
}
