package id.my.agungdh.simonjp.service;

import id.my.agungdh.simonjp.config.AppAuthProperties;
import id.my.agungdh.simonjp.entity.User;
import id.my.agungdh.simonjp.dto.LoginRequest;
import id.my.agungdh.simonjp.dto.MeResponse;
import id.my.agungdh.simonjp.mapper.UserMapper;
import id.my.agungdh.simonjp.repository.UserRepository;
import id.my.agungdh.simonjp.security.AuthenticatedUser;
import id.my.agungdh.simonjp.security.OpaqueTokenService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final OpaqueTokenService tokenService;
    private final UserMapper userMapper;
    private final AppAuthProperties properties;

    @Transactional(readOnly = true)
    public LoginResult login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> new BadCredentialsException("Username atau password salah"));

        String token = tokenService.issue(user.getId());
        return new LoginResult(token, properties.tokenTtl());
    }

    public void logout(String token) {
        if (token != null) {
            tokenService.revoke(token);
        }
    }

    @Transactional(readOnly = true)
    public MeResponse me(AuthenticatedUser principal) {
        User user = userRepository.findById(principal.id())
            .orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan"));
        return userMapper.toMeResponse(user);
    }

    public record LoginResult(String token, Duration expiresIn) {
    }
}
