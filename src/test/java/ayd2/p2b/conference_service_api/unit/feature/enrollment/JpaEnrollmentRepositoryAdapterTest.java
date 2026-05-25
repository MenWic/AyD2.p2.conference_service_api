package ayd2.p2b.conference_service_api.unit.feature.enrollment;

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
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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
    void save_delegates_to_repository_and_mapper() {
        UUID userId = UUID.randomUUID();
        UUID congressId = UUID.randomUUID();
        Enrollment enrollment = Enrollment.builder()
                .id(UUID.randomUUID()).congressId(congressId).userId(userId)
                .paymentId(UUID.randomUUID()).paymentDate(LocalDate.now()).createdBy(userId)
                .build();
        EnrollmentEntity entity = new EnrollmentEntity();
        EnrollmentEntity saved = new EnrollmentEntity();
        given(enrollmentMapper.toEntity(enrollment)).willReturn(entity);
        given(enrollmentJpaRepository.save(entity)).willReturn(saved);
        given(enrollmentMapper.toDomain(saved)).willReturn(enrollment);

        Enrollment result = adapter.save(enrollment);

        assertThat(result).isEqualTo(enrollment);
        verify(enrollmentJpaRepository).save(entity);
    }

    @Test
    void findByCongressIdAndUserId_returns_mapped_when_present() {
        UUID congressId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EnrollmentEntity entity = new EnrollmentEntity();
        Enrollment expected = Enrollment.builder()
                .id(UUID.randomUUID()).congressId(congressId).userId(userId)
                .paymentId(UUID.randomUUID()).paymentDate(LocalDate.now()).createdBy(userId)
                .build();
        given(enrollmentJpaRepository.findByCongressIdAndUserId(congressId, userId))
                .willReturn(Optional.of(entity));
        given(enrollmentMapper.toDomain(entity)).willReturn(expected);

        Optional<Enrollment> result = adapter.findByCongressIdAndUserId(congressId, userId);

        assertThat(result).isPresent().contains(expected);
    }

    @Test
    void findByCongressIdAndUserId_returns_empty_when_not_found() {
        UUID congressId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        given(enrollmentJpaRepository.findByCongressIdAndUserId(congressId, userId))
                .willReturn(Optional.empty());

        Optional<Enrollment> result = adapter.findByCongressIdAndUserId(congressId, userId);

        assertThat(result).isEmpty();
    }

    @Test
    void findByUserId_returns_mapped_page() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = Pageable.unpaged();
        EnrollmentEntity entity = new EnrollmentEntity();
        Enrollment enrollment = Enrollment.builder()
                .id(UUID.randomUUID()).congressId(UUID.randomUUID()).userId(userId)
                .paymentId(UUID.randomUUID()).paymentDate(LocalDate.now()).createdBy(userId)
                .build();
        given(enrollmentJpaRepository.findByUserId(userId, pageable))
                .willReturn(new PageImpl<>(List.of(entity)));
        given(enrollmentMapper.toDomain(entity)).willReturn(enrollment);

        Page<Enrollment> result = adapter.findByUserId(userId, pageable);

        assertThat(result.getContent()).containsExactly(enrollment);
    }

    @Test
    void findByCongressId_returns_mapped_page() {
        UUID congressId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Pageable pageable = Pageable.unpaged();
        EnrollmentEntity entity = new EnrollmentEntity();
        Enrollment enrollment = Enrollment.builder()
                .id(UUID.randomUUID()).congressId(congressId).userId(userId)
                .paymentId(UUID.randomUUID()).paymentDate(LocalDate.now()).createdBy(userId)
                .build();
        given(enrollmentJpaRepository.findByCongressId(congressId, pageable))
                .willReturn(new PageImpl<>(List.of(entity)));
        given(enrollmentMapper.toDomain(entity)).willReturn(enrollment);

        Page<Enrollment> result = adapter.findByCongressId(congressId, pageable);

        assertThat(result.getContent()).containsExactly(enrollment);
    }
}
