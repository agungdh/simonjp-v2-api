package id.my.agungdh.simonjp.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Konfigurasi auth opaque token (lihat {@code app.auth} di application.yaml).
 *
 * @param tokenTtl       masa berlaku token; dipakai juga sebagai Max-Age cookie.
 * @param renewThreshold ambang sliding renewal. Token diperpanjang bila sisa TTL
 *                       kurang dari {@code tokenTtl * renewThreshold}. Nilai <= 0
 *                       berarti selalu diperpanjang tiap request.
 * @param cookieName     nama cookie untuk klien web.
 * @param cookieSecure   set {@code true} bila dilayani via HTTPS.
 * @param cookieSameSite SameSite policy cookie (Lax/Strict/None).
 */
@ConfigurationProperties(prefix = "app.auth")
public record AppAuthProperties(
    Duration tokenTtl,
    double renewThreshold,
    String cookieName,
    boolean cookieSecure,
    String cookieSameSite
) {
}
