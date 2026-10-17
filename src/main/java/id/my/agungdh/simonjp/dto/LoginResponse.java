package id.my.agungdh.simonjp.dto;

import java.time.Duration;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresIn
) {
    public static LoginResponse of(String token, Duration ttl) {
        return new LoginResponse(token, "Bearer", ttl.toSeconds());
    }
}
