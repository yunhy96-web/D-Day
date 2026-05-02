package com.hauly.platform.auth.domain.service;

/**
 * Domain service interface for JWT token operations.
 * Application layer depends only on this interface; the infrastructure
 * concrete (JwtTokenProvider) implements it, keeping DDD layer rules intact.
 *
 * No Spring or jjwt imports — pure domain contract.
 */
public interface TokenService {

    /**
     * Issues a short-lived access token for the given user.
     *
     * @param userId user identifier
     * @param role   role string (e.g. "ADMIN", "INTAKE")
     * @return signed JWT access token
     */
    String issueAccessToken(Long userId, String role);

    /**
     * Issues a long-lived refresh token for the given user.
     *
     * @param userId user identifier
     * @param role   role string
     * @return signed JWT refresh token
     */
    String issueRefreshToken(Long userId, String role);

    /**
     * Validates a refresh token and returns its parsed claims.
     * Throws BadCredentialsException on failure.
     *
     * @param token the raw refresh token string
     * @return parsed token claims
     */
    TokenClaims validateRefreshToken(String token);

    /**
     * Parsed token claims returned by validation methods.
     *
     * @param userId user identifier extracted from subject
     * @param role   role claim
     */
    record TokenClaims(Long userId, String role) {}
}
