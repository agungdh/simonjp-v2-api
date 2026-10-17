package id.my.agungdh.simonjp.dto;

import java.util.UUID;

public record MeResponse(
    Long id,
    UUID uuid,
    String username,
    Long pegawaiId,
    String nama,
    String nip
) {
}
