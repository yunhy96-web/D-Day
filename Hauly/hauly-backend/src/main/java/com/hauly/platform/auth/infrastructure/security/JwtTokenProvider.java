package com.hauly.platform.auth.infrastructure.security;

import com.hauly.platform.auth.domain.service.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT token provider using jjwt 0.12.6 API.
 * Issues and validates access + refresh tokens.
 * Implements {@link TokenService} so the application layer depends only on the domain interface.
 *
 * Access tokens:  typ=access,  short-lived (${hauly.jwt.access-ttl-min} minutes)
 * Refresh tokens: typ=refresh, long-lived  (${hauly.jwt.refresh-ttl-day} days)
 */
@Component
public class JwtTokenProvider implements TokenService {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYP  = "typ";
    private static final String TYP_ACCESS  = "access";
    private static final String TYP_REFRESH = "refresh";

    private final SecretKey secretKey;
    private final long accessTtlMs;
    private final long refreshTtlMs;

    public JwtTokenProvider(
            @Value("${hauly.jwt.secret}") String secret,
            @Value("${hauly.jwt.access-ttl-min}") int accessTtlMin,
            @Value("${hauly.jwt.refresh-ttl-day}") int refreshTtlDay) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                "hauly.jwt.secret must be at least 32 bytes (256 bits) for HS256. " +
                "Current length: " + secretBytes.length + " bytes."
            );
        }
        this.secretKey = Keys.hmacShaKeyFor(secretBytes);
        this.accessTtlMs  = (long) accessTtlMin  * 60 * 1000;
        this.refreshTtlMs = (long) refreshTtlDay * 24 * 60 * 60 * 1000;
    }

    /** Issues a short-lived access token. */
    @Override
    public String issueAccessToken(Long userId, String role) {
        return buildToken(userId, role, TYP_ACCESS, accessTtlMs);
    }

    /** Issues a long-lived refresh token. */
    @Override
    public String issueRefreshToken(Long userId, String role) {
        return buildToken(userId, role, TYP_REFRESH, refreshTtlMs);
    }

    /** Validates an access token and returns its claims. Throws BadCredentialsException on failure. */
    public TokenService.TokenClaims validateAccessToken(String token) {
        Claims claims = parseClaims(token);
        if (!TYP_ACCESS.equals(claims.get(CLAIM_TYP, String.class))) {
            throw new BadCredentialsException("Token is not an access token");
        }
        return toTokenClaims(claims);
    }

    /** Validates a refresh token and returns its claims. Throws BadCredentialsException on failure. */
    @Override
    public TokenService.TokenClaims validateRefreshToken(String token) {
        Claims claims = parseClaims(token);
        if (!TYP_REFRESH.equals(claims.get(CLAIM_TYP, String.class))) {
            throw new BadCredentialsException("Token is not a refresh token");
        }
        return toTokenClaims(claims);
    }

    // -----------------------------------------------------------------------

    private String buildToken(Long userId, String role, String typ, long ttlMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMs);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TYP, typ)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BadCredentialsException("Token expired", e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid token", e);
        }
    }

    private TokenService.TokenClaims toTokenClaims(Claims claims) {
        Long userId = Long.parseLong(claims.getSubject());
        String role = claims.get(CLAIM_ROLE, String.class);
        return new TokenService.TokenClaims(userId, role);
    }
}
