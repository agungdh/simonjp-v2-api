package id.my.agungdh.simonjp.repository;

import id.my.agungdh.simonjp.entity.User;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    /**
     * Buat principal per request ({@code AuthenticatedUserService}); {@code pegawai} di-fetch
     * sekalian karena relasinya lazy dan {@code open-in-view} dimatikan.
     */
    @Query("select u from User u join fetch u.pegawai where u.id = :id")
    Optional<User> findByIdWithPegawai(@Param("id") Long id);

    /**
     * Soft delete: mengisi {@code deleted_at} + {@code deleted_by}. Query baca
     * otomatis terfilter oleh {@code @SQLRestriction("deleted_at is null")}.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update User u
           set u.deletedAt = :at, u.deletedBy = :by
         where u.id = :id and u.deletedAt is null
        """)
    int softDelete(@Param("id") Long id, @Param("at") Instant at, @Param("by") Long by);
}
