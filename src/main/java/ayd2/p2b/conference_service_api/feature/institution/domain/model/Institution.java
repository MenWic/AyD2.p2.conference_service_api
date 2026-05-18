package ayd2.p2b.conference_service_api.feature.institution.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class Institution {
    UUID id;
    String name;
    String description;
    String contactEmail;
    boolean active;
    UUID createdBy;
    LocalDateTime createdAt;
    UUID updatedBy;
    LocalDateTime updatedAt;
}
