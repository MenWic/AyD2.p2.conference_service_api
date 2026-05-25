package ayd2.p2b.conference_service_api.feature.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RosterEntry {
    private String personalId;
    private String fullName;
    private String email;
}
