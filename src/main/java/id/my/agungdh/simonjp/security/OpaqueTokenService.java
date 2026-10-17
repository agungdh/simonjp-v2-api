package id.my.agungdh.simonjp.security;

import id.my.agungdh.simonjp.config.AppAuthProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Opaque token: token acak 256-bit yang dikembalikan ke klien, sementara di Valkey
 * yang disimpan adalah hash SHA-256 dari token (raw token tidak pernah persist).
 */
@Service
public class OpaqueTokenService {

    private static final String KEY_PREFIX = "auth:sess:";

    private final StringRedisTemplate redis;
    private final AppAuthProperties properties;
    private final SecureRandom random = new SecureRandom();

    public OpaqueTokenService(StringRedisTemplate redis, AppAuthProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public String issue(AuthenticatedUser user) {
        String token = generateToken();
        redis.opsForValue().set(key(token), encode(user), properties.tokenTtl());
        return token;
    }

    public Optional<AuthenticatedUser> resolve(String token) {
        return Optional.ofNullable(redis.opsForValue().get(key(token)))
            .map(OpaqueTokenService::decode);
    }

    /**
     * Sliding renewal: perpanjang TTL bila sisa waktu sudah melewati ambang.
     *
     * @return {@code true} bila TTL benar-benar diperpanjang.
     */
    public boolean renewIfNeeded(String token) {
        String key = key(token);
        Long remainingSeconds = redis.getExpire(key, TimeUnit.SECONDS);
        if (remainingSeconds == null || remainingSeconds < 0) {
            return false;
        }
        long ttlSeconds = properties.tokenTtl().toSeconds();
        double threshold = properties.renewThreshold();
        boolean needsRenew = threshold <= 0 || remainingSeconds < ttlSeconds * threshold;
        if (!needsRenew) {
            return false;
        }
        redis.expire(key, properties.tokenTtl());
        return true;
    }

    public void revoke(String token) {
        redis.delete(key(token));
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String key(String token) {
        return KEY_PREFIX + sha256Hex(token);
    }

    // username dibatasi charset [A-Za-z0-9._-] lewat validasi DTO, jadi delimiter "|" aman.
    private static String encode(AuthenticatedUser user) {
        return user.id() + "|" + user.uuid() + "|" + user.username() + "|" + user.pegawaiId();
    }

    private static AuthenticatedUser decode(String value) {
        String[] parts = value.split("\\|", -1);
        return new AuthenticatedUser(
            Long.valueOf(parts[0]),
            UUID.fromString(parts[1]),
            parts[2],
            Long.valueOf(parts[3])
        );
    }

    private static String sha256Hex(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 tidak tersedia", e);
        }
    }
}
