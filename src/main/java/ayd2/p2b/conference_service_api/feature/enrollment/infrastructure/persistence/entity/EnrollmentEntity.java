package ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "enrollments")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class EnrollmentEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @ToString.Include
  private UUID id;

  @Column(name = "congress_id", nullable = false)
  private UUID congressId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "payment_id", nullable = false)
  private UUID paymentId;

  @Column(name = "enrolled_at", nullable = false)
  private Instant enrolledAt;

  @Column(name = "payment_date", nullable = false)
  private LocalDate paymentDate;

  @Column(name = "created_by", nullable = false)
  private UUID createdBy;

  @PrePersist
  void prePersist() {
    if (enrolledAt == null) {
      enrolledAt = Instant.now();
    }
  }
}
