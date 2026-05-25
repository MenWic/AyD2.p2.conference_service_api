package ayd2.p2b.conference_service_api.feature.attendance.dto.internal;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.UUID;

@Value
@Builder
public class AttendanceSearchCriteria {
    UUID activityId;
    UUID roomId;
    LocalDate dateFrom;
    LocalDate dateTo;
    String personalId;
}
