package ayd2.p2b.conference_service_api.feature.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CongressByInstitutionItem {
    private UUID institutionId;
    private String institutionName;
    private UUID congressId;
    private String congressName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String location;
    private BigDecimal price;
}
