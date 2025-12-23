package DdaySpringBoot.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private String timezone;
    private String nickname;
    private Long userId;
    private String role;

    public static TokenResponse of(String accessToken, String refreshToken, Long expiresIn, String timezone, String nickname, Long userId, String role) {
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .timezone(timezone)
                .nickname(nickname)
                .userId(userId)
                .role(role)
                .build();
    }
}
