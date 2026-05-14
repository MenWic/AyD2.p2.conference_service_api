package ayd2.p2b.conference_service_api.feature.institution.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UpdateInstitutionRequest", description = "Request payload to update an institution")
public class UpdateInstitutionRequest {

    @Schema(example = "Universidad de San Carlos")
    private String name;

    @Schema(example = "Updated institution description")
    private String description;

    @Schema(example = "contacto@usac.edu.gt")
    @Email(message = "contactEmail must be a valid email")
    private String contactEmail;
}
