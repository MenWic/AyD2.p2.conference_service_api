package ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.entity;

import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.entity.InstitutionEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "congresses")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class CongressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID id;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", insertable = false, updatable = false)
    private InstitutionEntity institution;

    @Column(name = "name", nullable = false, length = 255)
    @ToString.Include
    private String name;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "location", nullable = false, length = 500)
    private String location;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
