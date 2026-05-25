package ayd2.p2b.conference_service_api.feature.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceActivityItem {
    private UUID activityId;
    private String activityName;
    private String roomName;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private long attendanceCount;
}
