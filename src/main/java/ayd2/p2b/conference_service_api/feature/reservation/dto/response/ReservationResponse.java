package ayd2.p2b.conference_service_api.feature.reservation.dto.response;

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
@Schema(name = "ReservationResponse", description = "Reservation response payload")
public class ReservationResponse {

    @Schema(description = "Reservation id")
    private UUID id;

    @Schema(description = "Activity id")
    private UUID activityId;

    @Schema(description = "Participant user id")
    private UUID userId;

    @Schema(description = "Reservation timestamp")
    private OffsetDateTime reservedAt;
}
