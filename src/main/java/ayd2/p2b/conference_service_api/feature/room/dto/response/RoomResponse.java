package ayd2.p2b.conference_service_api.feature.room.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "RoomResponse", description = "Room API response payload")
public class RoomResponse {
    @Schema(example = "57bd53d0-60a9-4b5f-a52a-7de4013676bf")
    private UUID id;

    @Schema(example = "9470bdea-8e37-4d0e-b2ae-545211ec4498")
    private UUID congressId;

    @Schema(example = "Sala Magna")
    private String name;

    @Schema(example = "120")
    private Integer capacity;

    @Schema(example = "Edificio A, nivel 2")
    private String location;

    @Schema(example = "2026-05-22T10:15:30")
    private LocalDateTime createdAt;

    @Schema(example = "2026-05-22T11:30:45")
    private LocalDateTime updatedAt;
}
