package ayd2.p2b.conference_service_api.unit.feature.enrollment.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.enrollment.domain.model.Enrollment;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.adapter.JpaEnrollmentRepositoryAdapter;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.entity.EnrollmentEntity;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.repository.EnrollmentJpaRepository;
import ayd2.p2b.conference_service_api.feature.enrollment.mapper.EnrollmentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaEnrollmentRepositoryAdapterTest {

    @Mock
    private EnrollmentJpaRepository enrollmentJpaRepository;
    @Mock
    private EnrollmentMapper enrollmentMapper;

    private JpaEnrollmentRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaEnrollmentRepositoryAdapter(enrollmentJpaRepository, enrollmentMapper);
    }

    @Test
    void shouldSaveEnrollmentThroughRepositoryAndMapper() {
        Enrollment enrollment = sampleEnrollment();
        EnrollmentEntity entity = new EnrollmentEntity();
        EnrollmentEntity savedEntity = new EnrollmentEntity();
        savedEntity.setId(enrollment.getId());

        when(enrollmentMapper.toEntity(enrollment)).thenReturn(entity);
        when(enrollmentJpaRepository.save(entity)).thenReturn(savedEntity);
        when(enrollmentMapper.toDomain(savedEntity)).thenReturn(enrollment);

        Enrollment result = adapter.save(enrollment);

        assertThat(result).isEqualTo(enrollment);
    }

    @Test
    void shouldFindByCongressIdAndUserIdWhenFound() {
        UUID congressId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EnrollmentEntity entity = new EnrollmentEntity();
        Enrollment domain = sampleEnrollment();

        when(enrollmentJpaRepository.findByCongressIdAndUserId(congressId, userId)).thenReturn(Optional.of(entity));
        when(enrollmentMapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findByCongressIdAndUserId(congressId, userId)).contains(domain);
    }

    @Test
    void shouldReturnEmptyWhenFindByCongressIdAndUserIdIsMissing() {
        UUID congressId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(enrollmentJpaRepository.findByCongressIdAndUserId(congressId, userId)).thenReturn(Optional.empty());

        assertThat(adapter.findByCongressIdAndUserId(congressId, userId)).isEmpty();
    }

    @Test
    void shouldFindByUserIdAndMapPageContent() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 1);
        EnrollmentEntity entity = new EnrollmentEntity();
        entity.setId(UUID.randomUUID());
        Enrollment domain = Enrollment.builder()
                .id(entity.getId())
                .congressId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .paymentId(UUID.randomUUID())
                .enrolledAt(Instant.parse("2026-10-10T10:00:00Z"))
                .paymentDate(LocalDate.of(2026, 10, 10))
                .createdBy(UUID.randomUUID())
                .build();

        when(enrollmentJpaRepository.findByUserId(userId, pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 5));
        when(enrollmentMapper.toDomain(entity)).thenReturn(domain);

        Page<Enrollment> page = adapter.findByUserId(userId, pageable);

        assertThat(page.getContent()).containsExactly(domain);
        assertThat(page.getTotalElements()).isEqualTo(5);
    }

    @Test
    void shouldFindByCongressIdAndMapPageContent() {
        UUID congressId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 1);
        EnrollmentEntity entity = new EnrollmentEntity();
        entity.setId(UUID.randomUUID());
        Enrollment domain = Enrollment.builder()
                .id(entity.getId())
                .congressId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .paymentId(UUID.randomUUID())
                .enrolledAt(Instant.parse("2026-10-10T10:00:00Z"))
                .paymentDate(LocalDate.of(2026, 10, 10))
                .createdBy(UUID.randomUUID())
                .build();

        when(enrollmentJpaRepository.findByCongressId(congressId, pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 3));
        when(enrollmentMapper.toDomain(entity)).thenReturn(domain);

        Page<Enrollment> page = adapter.findByCongressId(congressId, pageable);

        assertThat(page.getContent()).containsExactly(domain);
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    private Enrollment sampleEnrollment() {
        return Enrollment.builder()
                .id(UUID.randomUUID())
                .congressId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .paymentId(UUID.randomUUID())
                .enrolledAt(Instant.parse("2026-10-10T10:00:00Z"))
                .paymentDate(LocalDate.of(2026, 10, 10))
                .createdBy(UUID.randomUUID())
                .build();
    }
}
