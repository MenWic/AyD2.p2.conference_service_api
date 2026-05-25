package ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.attendance.application.port.AttendanceRepositoryPort;
import ayd2.p2b.conference_service_api.feature.attendance.domain.model.Attendance;
import ayd2.p2b.conference_service_api.feature.attendance.dto.internal.AttendanceSearchCriteria;
import ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.repository.AttendanceRepository;
import ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.specification.AttendanceSpecification;
import ayd2.p2b.conference_service_api.feature.attendance.mapper.AttendanceMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
public class JpaAttendanceRepositoryAdapter implements AttendanceRepositoryPort {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceMapper attendanceMapper;

    public JpaAttendanceRepositoryAdapter(
            AttendanceRepository attendanceRepository,
            AttendanceMapper attendanceMapper
    ) {
        this.attendanceRepository = attendanceRepository;
        this.attendanceMapper = attendanceMapper;
    }

    @Override
    public Attendance save(Attendance attendance) {
        return attendanceMapper.toDomain(
                attendanceRepository.save(attendanceMapper.toEntity(attendance))
        );
    }

    @Override
    public boolean existsByActivityIdAndUserId(UUID activityId, UUID userId) {
        return attendanceRepository.existsByActivityIdAndUserId(activityId, userId);
    }

    @Override
    public Page<Attendance> findByCriteria(
            AttendanceSearchCriteria criteria,
            Pageable pageable,
            UUID requesterUserId,
            Set<UUID> linkedInstitutionIds
    ) {
        return attendanceRepository.findAll(
                        AttendanceSpecification.byCriteriaAndScope(criteria, requesterUserId, linkedInstitutionIds),
                        pageable
                )
                .map(attendanceMapper::toDomain);
    }
}
