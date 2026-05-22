package com.flasharena.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * HS256 JWT 발급/검증. sub=userId, custom claim=role, issuer=flasharena.
 * jjwt 0.12.x API 사용.
 */
@Component
public class JwtProvider {

    private static final String ISSUER = "flasharena";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final long expirationMs;

    public JwtProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /** 토큰 발급. sub 에 userId, role 클레임에 권한을 싣는다. */
    public String generateToken(UUID userId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(userId.toString())
                .claim(CLAIM_ROLE, role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /** 토큰 검증 후 userId/role 추출. 유효하지 않거나 만료 시 InvalidTokenException. */
    public AuthPayload parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(ISSUER)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            UUID userId = UUID.fromString(claims.getSubject());
            String role = claims.get(CLAIM_ROLE, String.class);
            return new AuthPayload(userId, role);
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("유효하지 않거나 만료된 토큰입니다.", e);
        }
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }

    /** JWT 에서 뽑아낸 인증 주체 정보. */
    public record AuthPayload(UUID userId, String role) {
    }

    /** 토큰 검증 실패 시 던지는 예외. */
    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
