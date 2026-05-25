package ayd2.p2b.conference_service_api.feature.attendance.application.user;

import ayd2.p2b.conference_service_api.feature.attendance.dto.response.AttendanceResponse;
import ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.repository.AttendanceRepository;
import ayd2.p2b.conference_service_api.feature.attendance.mapper.AttendanceMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class GetUserAttendanceUseCase {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceMapper attendanceMapper;

    public GetUserAttendanceUseCase(AttendanceRepository attendanceRepository, AttendanceMapper attendanceMapper) {
        this.attendanceRepository = attendanceRepository;
        this.attendanceMapper = attendanceMapper;
    }

    public List<AttendanceResponse> execute(UUID userId) {
        return attendanceRepository.findByUserIdOrderByRegisteredAtDesc(userId)
                .stream()
                .map(e -> attendanceMapper.toResponse(attendanceMapper.toDomain(e)))
                .toList();
    }
}
