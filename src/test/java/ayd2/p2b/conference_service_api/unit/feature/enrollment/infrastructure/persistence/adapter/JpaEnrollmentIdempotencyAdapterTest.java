package ayd2.p2b.conference_service_api.unit.feature.enrollment.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.feature.enrollment.domain.model.EnrollmentIdempotencyRecord;
import ayd2.p2b.conference_service_api.feature.enrollment.domain.model.EnrollmentIdempotencyStatus;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.adapter.JpaEnrollmentIdempotencyAdapter;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.entity.EnrollmentIdempotencyRecordEntity;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.repository.EnrollmentIdempotencyJpaRepository;
import ayd2.p2b.conference_service_api.feature.enrollment.mapper.EnrollmentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaEnrollmentIdempotencyAdapterTest {

    @Mock
    private EnrollmentIdempotencyJpaRepository idempotencyJpaRepository;
    @Mock
    private EnrollmentMapper enrollmentMapper;

    private JpaEnrollmentIdempotencyAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaEnrollmentIdempotencyAdapter(idempotencyJpaRepository, enrollmentMapper);
    }

    @Test
    void shouldInsertRecordThroughMapperAndRepository() {
        EnrollmentIdempotencyRecord record = sampleRecord();
        EnrollmentIdempotencyRecordEntity entity = new EnrollmentIdempotencyRecordEntity();
        EnrollmentIdempotencyRecordEntity savedEntity = new EnrollmentIdempotencyRecordEntity();
        EnrollmentIdempotencyRecord mappedDomain = sampleRecord();

        when(enrollmentMapper.toIdempotencyEntity(record)).thenReturn(entity);
        when(idempotencyJpaRepository.saveAndFlush(entity)).thenReturn(savedEntity);
        when(enrollmentMapper.toIdempotencyDomain(savedEntity)).thenReturn(mappedDomain);

        EnrollmentIdempotencyRecord result = adapter.insert(record);

        assertThat(result).isEqualTo(mappedDomain);
    }

    @Test
    void shouldMapDuplicateInsertToConflictApiException() {
        EnrollmentIdempotencyRecord record = sampleRecord();
        EnrollmentIdempotencyRecordEntity entity = new EnrollmentIdempotencyRecordEntity();

        when(enrollmentMapper.toIdempotencyEntity(record)).thenReturn(entity);
        when(idempotencyJpaRepository.saveAndFlush(entity)).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> adapter.insert(record))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.getCode()).isEqualTo("resource.conflict");
                });
    }

    @Test
    void shouldFindByKeyWhenFound() {
        String key = "idem-key-1";
        EnrollmentIdempotencyRecordEntity entity = new EnrollmentIdempotencyRecordEntity();
        EnrollmentIdempotencyRecord domain = sampleRecord();

        when(idempotencyJpaRepository.findById(key)).thenReturn(Optional.of(entity));
        when(enrollmentMapper.toIdempotencyDomain(entity)).thenReturn(domain);

        assertThat(adapter.findByKey(key)).contains(domain);
    }

    @Test
    void shouldReturnEmptyWhenFindByKeyIsMissing() {
        String key = "idem-key-2";
        when(idempotencyJpaRepository.findById(key)).thenReturn(Optional.empty());

        assertThat(adapter.findByKey(key)).isEmpty();
    }

    @Test
    void shouldFindActiveByCongressIdAndUserIdWhenFound() {
        UUID congressId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EnrollmentIdempotencyRecordEntity entity = new EnrollmentIdempotencyRecordEntity();
        EnrollmentIdempotencyRecord domain = sampleRecord();

        when(idempotencyJpaRepository.findActiveByCongressIdAndUserId(congressId, userId)).thenReturn(Optional.of(entity));
        when(enrollmentMapper.toIdempotencyDomain(entity)).thenReturn(domain);

        assertThat(adapter.findActiveByCongressIdAndUserId(congressId, userId)).contains(domain);
    }

    @Test
    void shouldReturnEmptyWhenFindActiveByCongressIdAndUserIdIsMissing() {
        UUID congressId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(idempotencyJpaRepository.findActiveByCongressIdAndUserId(congressId, userId)).thenReturn(Optional.empty());

        assertThat(adapter.findActiveByCongressIdAndUserId(congressId, userId)).isEmpty();
    }

    @Test
    void shouldUpdateRecordThroughMapperAndRepository() {
        EnrollmentIdempotencyRecord record = sampleRecord();
        EnrollmentIdempotencyRecordEntity entity = new EnrollmentIdempotencyRecordEntity();
        EnrollmentIdempotencyRecordEntity savedEntity = new EnrollmentIdempotencyRecordEntity();
        EnrollmentIdempotencyRecord mappedDomain = sampleRecord();

        when(enrollmentMapper.toIdempotencyEntity(record)).thenReturn(entity);
        when(idempotencyJpaRepository.save(entity)).thenReturn(savedEntity);
        when(enrollmentMapper.toIdempotencyDomain(savedEntity)).thenReturn(mappedDomain);

        EnrollmentIdempotencyRecord result = adapter.update(record);

        assertThat(result).isEqualTo(mappedDomain);
        verify(idempotencyJpaRepository).save(entity);
    }

    private EnrollmentIdempotencyRecord sampleRecord() {
        return EnrollmentIdempotencyRecord.builder()
                .idempotencyKey("idem-key")
                .congressId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .paymentDate(LocalDate.of(2026, 10, 10))
                .requestHash("abc123")
                .status(EnrollmentIdempotencyStatus.PROCESSING)
                .enrollmentId(UUID.randomUUID())
                .paymentId(UUID.randomUUID())
                .institutionId(UUID.randomUUID())
                .congressNameSnapshot("Congreso")
                .institutionNameSnapshot("Institucion")
                .amount(new BigDecimal("80.00"))
                .createdAt(Instant.parse("2026-10-10T10:00:00Z"))
                .updatedAt(Instant.parse("2026-10-10T10:00:00Z"))
                .build();
    }
}
