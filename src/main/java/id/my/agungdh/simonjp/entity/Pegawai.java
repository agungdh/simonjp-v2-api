package id.my.agungdh.simonjp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "pegawai")
@SQLRestriction("deleted_at is null")
@Getter
@Setter
@NoArgsConstructor
public class Pegawai extends BaseEntity {

    @Column(name = "nama", nullable = false)
    private String nama;

    @Column(name = "nip", nullable = false)
    private String nip;
}
