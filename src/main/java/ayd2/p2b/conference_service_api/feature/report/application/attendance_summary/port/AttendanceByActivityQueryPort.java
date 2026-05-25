package ayd2.p2b.conference_service_api.feature.report.application.attendance_summary.port;

import ayd2.p2b.conference_service_api.feature.report.dto.response.AttendanceActivityItem;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AttendanceByActivityQueryPort {
    List<AttendanceActivityItem> query(UUID congressId, UUID activityIdFilter, UUID roomIdFilter,
                                       OffsetDateTime dateFrom, OffsetDateTime dateTo);
}
