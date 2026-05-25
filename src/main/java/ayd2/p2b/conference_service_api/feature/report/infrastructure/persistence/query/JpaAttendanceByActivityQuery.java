package ayd2.p2b.conference_service_api.feature.report.infrastructure.persistence.query;

import ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.repository.AttendanceJpaRepository;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.repository.ActivityRepository;
import ayd2.p2b.conference_service_api.feature.room.infrastructure.persistence.repository.RoomRepository;
import ayd2.p2b.conference_service_api.feature.report.application.attendance_summary.port.AttendanceByActivityQueryPort;
import ayd2.p2b.conference_service_api.feature.report.dto.response.AttendanceActivityItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaAttendanceByActivityQuery implements AttendanceByActivityQueryPort {

    private final ActivityRepository activityRepository;
    private final RoomRepository roomRepository;
    private final AttendanceJpaRepository attendanceRepository;

    @Override
    public List<AttendanceActivityItem> query(UUID congressId, UUID activityIdFilter, UUID roomIdFilter,
                                               OffsetDateTime dateFrom, OffsetDateTime dateTo) {
        List<AttendanceActivityItem> result = new ArrayList<>();

        activityRepository.findAll().stream()
                .filter(a -> congressId.equals(a.getCongressId()))
                .filter(a -> activityIdFilter == null || activityIdFilter.equals(a.getId()))
                .filter(a -> roomIdFilter == null || roomIdFilter.equals(a.getRoomId()))
                .filter(a -> dateFrom == null || !a.getStartTime().isBefore(dateFrom))
                .filter(a -> dateTo == null || !a.getStartTime().isAfter(dateTo))
                .forEach(activity -> {
                    String roomName = roomRepository.findById(activity.getRoomId())
                            .map(r -> r.getName())
                            .orElse("");
                    long count = attendanceRepository.countByActivityId(activity.getId());
                    result.add(AttendanceActivityItem.builder()
                            .activityId(activity.getId())
                            .activityName(activity.getName())
                            .roomName(roomName)
                            .startTime(activity.getStartTime())
                            .endTime(activity.getEndTime())
                            .attendanceCount(count)
                            .build());
                });

        return result;
    }
}
