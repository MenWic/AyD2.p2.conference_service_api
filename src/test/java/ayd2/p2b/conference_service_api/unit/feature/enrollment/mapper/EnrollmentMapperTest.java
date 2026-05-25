package ayd2.p2b.conference_service_api.unit.feature.enrollment.mapper;

import ayd2.p2b.conference_service_api.feature.enrollment.domain.model.Enrollment;
import ayd2.p2b.conference_service_api.feature.enrollment.domain.model.EnrollmentIdempotencyRecord;
import ayd2.p2b.conference_service_api.feature.enrollment.domain.model.EnrollmentIdempotencyStatus;
import ayd2.p2b.conference_service_api.feature.enrollment.dto.response.EnrollmentResponse;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.entity.EnrollmentEntity;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.entity.EnrollmentIdempotencyRecordEntity;
import ayd2.p2b.conference_service_api.feature.enrollment.mapper.EnrollmentMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EnrollmentMapperTest {

    private final EnrollmentMapper mapper = new EnrollmentMapper() {
        @Override
        public Enrollment toDomain(EnrollmentEntity entity) {
            return null;
        }

        @Override
        public EnrollmentEntity toEntity(Enrollment enrollment) {
            return null;
        }

        @Override
        public EnrollmentResponse toResponse(Enrollment enrollment) {
            return null;
        }
    };

    @Test
    void shouldReturnNullDomainWhenIdempotencyEntityIsNull() {
        assertThat(mapper.toIdempotencyDomain(null)).isNull();
    }

    @Test
    void shouldMapAllFieldsFromIdempotencyEntityToDomain() {
        UUID congressId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-10T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-10-10T11:00:00Z");
        BigDecimal amount = new BigDecimal("99.99");

        EnrollmentIdempotencyRecordEntity entity = new EnrollmentIdempotencyRecordEntity();
        entity.setIdempotencyKey("key-1");
        entity.setCongressId(congressId);
        entity.setUserId(userId);
        entity.setPaymentDate(LocalDate.of(2026, 10, 10));
        entity.setRequestHash("hash-1");
        entity.setStatus("SUCCEEDED");
        entity.setEnrollmentId(enrollmentId);
        entity.setPaymentId(paymentId);
        entity.setInstitutionId(institutionId);
        entity.setCongressNameSnapshot("Congreso");
        entity.setInstitutionNameSnapshot("Institucion");
        entity.setAmount(amount);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);

        EnrollmentIdempotencyRecord domain = mapper.toIdempotencyDomain(entity);

        assertThat(domain.getIdempotencyKey()).isEqualTo("key-1");
        assertThat(domain.getCongressId()).isEqualTo(congressId);
        assertThat(domain.getUserId()).isEqualTo(userId);
        assertThat(domain.getPaymentDate()).isEqualTo(LocalDate.of(2026, 10, 10));
        assertThat(domain.getRequestHash()).isEqualTo("hash-1");
        assertThat(domain.getStatus()).isEqualTo(EnrollmentIdempotencyStatus.SUCCEEDED);
        assertThat(domain.getEnrollmentId()).isEqualTo(enrollmentId);
        assertThat(domain.getPaymentId()).isEqualTo(paymentId);
        assertThat(domain.getInstitutionId()).isEqualTo(institutionId);
        assertThat(domain.getCongressNameSnapshot()).isEqualTo("Congreso");
        assertThat(domain.getInstitutionNameSnapshot()).isEqualTo("Institucion");
        assertThat(domain.getAmount()).isEqualTo(amount);
        assertThat(domain.getCreatedAt()).isEqualTo(createdAt);
        assertThat(domain.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void shouldReturnNullEntityWhenIdempotencyRecordIsNull() {
        assertThat(mapper.toIdempotencyEntity(null)).isNull();
    }

    @Test
    void shouldMapAllFieldsFromIdempotencyRecordToEntity() {
        UUID congressId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-10T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-10-10T11:00:00Z");
        BigDecimal amount = new BigDecimal("150.00");

        EnrollmentIdempotencyRecord record = EnrollmentIdempotencyRecord.builder()
                .idempotencyKey("key-2")
                .congressId(congressId)
                .userId(userId)
                .paymentDate(LocalDate.of(2026, 11, 1))
                .requestHash("hash-2")
                .status(EnrollmentIdempotencyStatus.FAILED)
                .enrollmentId(enrollmentId)
                .paymentId(paymentId)
                .institutionId(institutionId)
                .congressNameSnapshot("Congreso 2")
                .institutionNameSnapshot("Institucion 2")
                .amount(amount)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        EnrollmentIdempotencyRecordEntity entity = mapper.toIdempotencyEntity(record);

        assertThat(entity.getIdempotencyKey()).isEqualTo("key-2");
        assertThat(entity.getCongressId()).isEqualTo(congressId);
        assertThat(entity.getUserId()).isEqualTo(userId);
        assertThat(entity.getPaymentDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(entity.getRequestHash()).isEqualTo("hash-2");
        assertThat(entity.getStatus()).isEqualTo("FAILED");
        assertThat(entity.getEnrollmentId()).isEqualTo(enrollmentId);
        assertThat(entity.getPaymentId()).isEqualTo(paymentId);
        assertThat(entity.getInstitutionId()).isEqualTo(institutionId);
        assertThat(entity.getCongressNameSnapshot()).isEqualTo("Congreso 2");
        assertThat(entity.getInstitutionNameSnapshot()).isEqualTo("Institucion 2");
        assertThat(entity.getAmount()).isEqualTo(amount);
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
    }
}
