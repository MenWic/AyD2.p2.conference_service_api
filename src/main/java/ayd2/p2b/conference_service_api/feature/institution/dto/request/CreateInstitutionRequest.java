package ayd2.p2b.conference_service_api.feature.institution.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreateInstitutionRequest", description = "Request payload to create an institution")
public class CreateInstitutionRequest {

    @NotBlank(message = "name is required")
    @Schema(example = "Universidad de San Carlos")
    private String name;

    @NotBlank(message = "description is required")
    @Schema(example = "Public university institution")
    private String description;

    @NotBlank(message = "contactEmail is required")
    @Email(message = "contactEmail must be a valid email")
    @Schema(example = "contacto@usac.edu.gt")
    private String contactEmail;
}
