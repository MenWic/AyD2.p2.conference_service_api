package ayd2.p2b.conference_service_api.feature.room.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UpdateRoomRequest", description = "Request payload to update an existing room")
public class UpdateRoomRequest {

    @Schema(example = "Sala Magna Norte")
    private String name;

    @Positive(message = "capacity must be > 0")
    @Schema(example = "140")
    private Integer capacity;

    @Schema(example = "Edificio B, nivel 1")
    private String location;
}
