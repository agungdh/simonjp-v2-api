package id.my.agungdh.simonjp.security;

import java.time.Duration;
import org.springframework.http.ResponseCookie;

public final class AuthCookies {

    private AuthCookies() {
    }

    public static ResponseCookie session(String name, String value, Duration maxAge, boolean secure, String sameSite) {
        return ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/")
            .maxAge(maxAge)
            .build();
    }

    public static ResponseCookie clear(String name, boolean secure, String sameSite) {
        return ResponseCookie.from(name, "")
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/")
            .maxAge(0)
            .build();
    }
}
