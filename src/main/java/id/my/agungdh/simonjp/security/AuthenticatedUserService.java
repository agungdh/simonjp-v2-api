package id.my.agungdh.simonjp.security;

import id.my.agungdh.simonjp.entity.User;
import id.my.agungdh.simonjp.repository.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Membangun ulang principal dari DB berdasarkan id user yang tersimpan di session Valkey
 * (lihat {@code OpaqueTokenService.resolve}). Dipakai filter tiap request, jadi session
 * tidak pernah basi: user yang di-soft-delete langsung ditolak dan perubahan datanya
 * (mis. username/pegawai) ikut terpakai.
 */
@Service
public class AuthenticatedUserService {

    private final UserRepository userRepository;

    public AuthenticatedUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * @return principal, atau kosong bila user sudah tidak ada / soft-deleted — session
     *         dianggap tidak valid lagi.
     */
    @Transactional(readOnly = true)
    public Optional<AuthenticatedUser> load(Long userId) {
        return userRepository.findByIdWithPegawai(userId).map(AuthenticatedUserService::toPrincipal);
    }

    // Relasi pegawai ikut di-fetch karena open-in-view: false -> akses lazy di luar
    // transaksi ini bakal gagal.
    private static AuthenticatedUser toPrincipal(User user) {
        return new AuthenticatedUser(
            user.getId(),
            user.getUuid(),
            user.getUsername(),
            user.getPegawai().getId()
        );
    }
}
