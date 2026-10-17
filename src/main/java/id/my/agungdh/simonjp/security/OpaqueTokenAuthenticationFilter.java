package id.my.agungdh.simonjp.security;

import id.my.agungdh.simonjp.config.AppAuthProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Membaca opaque token (header Bearer untuk mobile/desktop, cookie untuk web),
 * memvalidasinya ke Valkey, lalu mengisi {@code SecurityContext}. TTL token
 * diperpanjang secara sliding saat request terautentikasi.
 */
@Component
public class OpaqueTokenAuthenticationFilter extends OncePerRequestFilter {

    private final OpaqueTokenService tokenService;
    private final AppAuthProperties properties;

    public OpaqueTokenAuthenticationFilter(OpaqueTokenService tokenService, AppAuthProperties properties) {
        this.tokenService = tokenService;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String cookieToken = null;
            String token = TokenResolver.resolveHeader(request);
            boolean viaCookie = false;
            if (token == null) {
                cookieToken = TokenResolver.resolveCookie(request, properties.cookieName());
                token = cookieToken;
                viaCookie = token != null;
            }

            if (token != null) {
                Optional<AuthenticatedUser> user = tokenService.resolve(token);
                if (user.isPresent()) {
                    authenticate(request, user.get());
                    if (tokenService.renewIfNeeded(token) && viaCookie) {
                        response.addHeader(HttpHeaders.SET_COOKIE, AuthCookies
                            .session(properties.cookieName(), token, properties.tokenTtl(),
                                properties.cookieSecure(), properties.cookieSameSite())
                            .toString());
                    }
                } else if (viaCookie) {
                    response.addHeader(HttpHeaders.SET_COOKIE, AuthCookies
                        .clear(properties.cookieName(), properties.cookieSecure(), properties.cookieSameSite())
                        .toString());
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, AuthenticatedUser user) {
        var authentication = new UsernamePasswordAuthenticationToken(
            user, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
