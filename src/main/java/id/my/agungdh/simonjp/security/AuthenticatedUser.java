package id.my.agungdh.simonjp.security;

import java.util.UUID;

/**
 * Principal yang disimpan di {@code SecurityContext} sekaligus isi session di Valkey.
 * Sengaja bukan {@code UserDetails} (password tidak pernah disimpan di session).
 */
public record AuthenticatedUser(
    Long id,
    UUID uuid,
    String username,
    Long pegawaiId
) {
}
