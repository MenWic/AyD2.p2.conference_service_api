package ayd2.p2b.conference_service_api.feature.congress.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CongressResponse", description = "Congress response payload")
public class CongressResponse {
    private UUID id;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String location;
    private BigDecimal price;
    private UUID institutionId;
    private String institutionName;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
