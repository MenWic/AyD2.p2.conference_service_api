package ayd2.p2b.conference_service_api.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IamInternalUserIdentityResponse {
    private UUID id;
    private String personalId;
}
