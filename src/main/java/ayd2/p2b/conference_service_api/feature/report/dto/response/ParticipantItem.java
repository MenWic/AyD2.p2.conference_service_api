package ayd2.p2b.conference_service_api.feature.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantItem {
    private String personalId;
    private String fullName;
    private String organization;
    private String email;
    private String phone;
    private List<ParticipationTypeEnum> participationTypes;
}
