package ayd2.p2b.conference_service_api.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IamUserResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String personalId;
    private Boolean active;
    private Set<String> roles;
    private Set<UUID> linkedInstitutions;
}
