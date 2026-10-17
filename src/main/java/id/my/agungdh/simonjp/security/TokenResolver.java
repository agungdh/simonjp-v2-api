package id.my.agungdh.simonjp.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

/** Ambil token dari header {@code Authorization: Bearer} atau dari cookie. */
public final class TokenResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    private TokenResolver() {
    }

    public static String resolveHeader(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    public static String resolveCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isEmpty()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public static String resolve(HttpServletRequest request, String cookieName) {
        String fromHeader = resolveHeader(request);
        return fromHeader != null ? fromHeader : resolveCookie(request, cookieName);
    }
}
