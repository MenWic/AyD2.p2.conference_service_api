package ayd2.p2b.conference_service_api.feature.attendance.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "AttendanceResponse", description = "Attendance response payload")
public class AttendanceResponse {

    @Schema(description = "Attendance id")
    private UUID id;

    @Schema(description = "Activity id")
    private UUID activityId;

    @Schema(description = "Participant personal id snapshot")
    private String personalId;

    @Schema(description = "Congress admin user id that registered the attendance")
    private UUID registeredBy;

    @Schema(description = "Attendance registration timestamp")
    private OffsetDateTime registeredAt;
}
