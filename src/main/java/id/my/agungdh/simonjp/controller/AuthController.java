package id.my.agungdh.simonjp.controller;

import id.my.agungdh.simonjp.config.AppAuthProperties;
import id.my.agungdh.simonjp.dto.LoginRequest;
import id.my.agungdh.simonjp.dto.LoginResponse;
import id.my.agungdh.simonjp.dto.MeResponse;
import id.my.agungdh.simonjp.security.AuthCookies;
import id.my.agungdh.simonjp.security.AuthenticatedUser;
import id.my.agungdh.simonjp.security.TokenResolver;
import id.my.agungdh.simonjp.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AppAuthProperties properties;

    public AuthController(AuthService authService, AppAuthProperties properties) {
        this.authService = authService;
        this.properties = properties;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthService.LoginResult result = authService.login(request);

        // Cookie untuk klien web; token di body dipakai klien mobile/desktop via Bearer.
        response.addHeader(HttpHeaders.SET_COOKIE, AuthCookies
            .session(properties.cookieName(), result.token(), result.expiresIn(),
                properties.cookieSecure(), properties.cookieSameSite())
            .toString());

        return ResponseEntity.ok(LoginResponse.of(result.token(), result.expiresIn()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(TokenResolver.resolve(request, properties.cookieName()));
        response.addHeader(HttpHeaders.SET_COOKIE, AuthCookies
            .clear(properties.cookieName(), properties.cookieSecure(), properties.cookieSameSite())
            .toString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return authService.me(principal);
    }
}
