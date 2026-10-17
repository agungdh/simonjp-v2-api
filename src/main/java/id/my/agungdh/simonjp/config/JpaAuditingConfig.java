package id.my.agungdh.simonjp.config;

import id.my.agungdh.simonjp.security.AuthenticatedUser;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    /**
     * Sumber nilai kolom audit {@code *_by}: id user yang sedang login, atau kosong
     * (null) untuk operasi sistem/seed.
     */
    @Bean
    AuditorAware<Long> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
                return Optional.empty();
            }
            return Optional.of(user.id());
        };
    }
}
