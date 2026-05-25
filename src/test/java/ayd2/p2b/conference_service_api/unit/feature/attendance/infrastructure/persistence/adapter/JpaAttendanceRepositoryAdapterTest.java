package ayd2.p2b.conference_service_api.unit.feature.attendance.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.attendance.domain.model.Attendance;
import ayd2.p2b.conference_service_api.feature.attendance.dto.internal.AttendanceSearchCriteria;
import ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.adapter.JpaAttendanceRepositoryAdapter;
import ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.entity.AttendanceEntity;
import ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.repository.AttendanceRepository;
import ayd2.p2b.conference_service_api.feature.attendance.mapper.AttendanceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaAttendanceRepositoryAdapterTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private AttendanceMapper attendanceMapper;

    private JpaAttendanceRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaAttendanceRepositoryAdapter(attendanceRepository, attendanceMapper);
    }

    @Test
    void shouldSaveAttendanceThroughRepositoryAndMapper() {
        Attendance domain = Attendance.builder()
                .id(UUID.randomUUID())
                .activityId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .personalIdSnapshot("PID-100")
                .registeredBy(UUID.randomUUID())
                .createdBy(UUID.randomUUID())
                .build();
        AttendanceEntity entity = new AttendanceEntity();
        AttendanceEntity savedEntity = new AttendanceEntity();
        savedEntity.setId(domain.getId());

        when(attendanceMapper.toEntity(domain)).thenReturn(entity);
        when(attendanceRepository.save(entity)).thenReturn(savedEntity);
        when(attendanceMapper.toDomain(savedEntity)).thenReturn(domain);

        Attendance result = adapter.save(domain);

        assertThat(result).isEqualTo(domain);
    }

    @Test
    void shouldDelegateExistsByActivityIdAndUserIdReturningTrue() {
        UUID activityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(attendanceRepository.existsByActivityIdAndUserId(activityId, userId)).thenReturn(true);

        boolean exists = adapter.existsByActivityIdAndUserId(activityId, userId);

        assertThat(exists).isTrue();
        verify(attendanceRepository).existsByActivityIdAndUserId(activityId, userId);
    }

    @Test
    void shouldDelegateExistsByActivityIdAndUserIdReturningFalse() {
        UUID activityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(attendanceRepository.existsByActivityIdAndUserId(activityId, userId)).thenReturn(false);

        boolean exists = adapter.existsByActivityIdAndUserId(activityId, userId);

        assertThat(exists).isFalse();
        verify(attendanceRepository).existsByActivityIdAndUserId(activityId, userId);
    }

    @Test
    void shouldFindByCriteriaUsingSpecificationAndMapPageContent() {
        UUID requester = UUID.randomUUID();
        UUID linkedInstitution = UUID.randomUUID();
        AttendanceSearchCriteria criteria = AttendanceSearchCriteria.builder()
                .personalId("PID-400")
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        AttendanceEntity firstEntity = new AttendanceEntity();
        firstEntity.setId(UUID.randomUUID());
        AttendanceEntity secondEntity = new AttendanceEntity();
        secondEntity.setId(UUID.randomUUID());

        Attendance firstDomain = Attendance.builder().id(firstEntity.getId()).build();
        Attendance secondDomain = Attendance.builder().id(secondEntity.getId()).build();

        when(attendanceRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(firstEntity, secondEntity), pageable, 2));
        when(attendanceMapper.toDomain(firstEntity)).thenReturn(firstDomain);
        when(attendanceMapper.toDomain(secondEntity)).thenReturn(secondDomain);

        Page<Attendance> result = adapter.findByCriteria(criteria, pageable, requester, Set.of(linkedInstitution));

        ArgumentCaptor<Specification<AttendanceEntity>> specificationCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(attendanceRepository).findAll(specificationCaptor.capture(), eq(pageable));
        assertThat(specificationCaptor.getValue()).isNotNull();
        assertThat(result.getContent()).extracting(Attendance::getId)
                .containsExactly(firstEntity.getId(), secondEntity.getId());
        assertThat(result.getTotalElements()).isEqualTo(2);
    }
}
