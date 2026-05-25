package ayd2.p2b.conference_service_api.feature.attendance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "RegisterAttendanceRequest", description = "Request payload to register participant attendance")
public class RegisterAttendanceRequest {

    @NotNull(message = "activityId is required")
    @Schema(description = "Activity id where attendance is being registered")
    private UUID activityId;

    @NotBlank(message = "personalId is required")
    @Size(max = 50, message = "personalId must not exceed 50 characters")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "personalId must be alphanumeric")
    @Schema(description = "Participant personal id")
    private String personalId;
}
