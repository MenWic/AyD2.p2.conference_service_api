package ayd2.p2b.conference_service_api.feature.room.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreateRoomRequest", description = "Request payload to create a room inside a congress")
public class CreateRoomRequest {

    @NotBlank(message = "name is required")
    @Schema(example = "Sala Magna")
    private String name;

    @Positive(message = "capacity must be > 0")
    @Schema(example = "120")
    private Integer capacity;

    @Schema(example = "Edificio A, nivel 2")
    private String location;
}
