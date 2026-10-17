package id.my.agungdh.simonjp.security;

import java.util.UUID;

/**
 * Principal yang diisi ke {@code SecurityContext}. Dibangun ulang dari DB tiap request
 * (session Valkey cuma menyimpan id user), sengaja bukan {@code UserDetails} dan tidak
 * pernah membawa password.
 */
public record AuthenticatedUser(
    Long id,
    UUID uuid,
    String username,
    Long pegawaiId
) {
}
