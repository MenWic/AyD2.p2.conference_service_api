package ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "enrollment_idempotency_records")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class EnrollmentIdempotencyRecordEntity {

  @Id
  @Column(name = "idempotency_key", nullable = false, length = 120)
  @ToString.Include
  private String idempotencyKey;

  @Column(name = "congress_id", nullable = false)
  private UUID congressId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "payment_date", nullable = false)
  private LocalDate paymentDate;

  @Column(name = "request_hash", nullable = false, length = 64)
  private String requestHash;

  @Column(name = "status", nullable = false, length = 20)
  private String status;

  @Column(name = "enrollment_id")
  private UUID enrollmentId;

  @Column(name = "payment_id")
  private UUID paymentId;

  @Column(name = "institution_id", nullable = false)
  private UUID institutionId;

  @Column(name = "congress_name_snapshot", nullable = false, length = 255)
  private String congressNameSnapshot;

  @Column(name = "institution_name_snapshot", nullable = false, length = 255)
  private String institutionNameSnapshot;

  @Column(name = "amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void prePersist() {
    Instant now = Instant.now();
    if (createdAt == null) {
      createdAt = now;
    }
    if (updatedAt == null) {
      updatedAt = now;
    }
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = Instant.now();
  }
}
