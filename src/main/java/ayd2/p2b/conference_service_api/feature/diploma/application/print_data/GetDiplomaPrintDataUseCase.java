package ayd2.p2b.conference_service_api.feature.diploma.application.print_data;

import ayd2.p2b.conference_service_api.feature.diploma.application.exception.DiplomaExceptions;
import ayd2.p2b.conference_service_api.feature.diploma.application.port.DiplomaRepositoryPort;
import ayd2.p2b.conference_service_api.feature.diploma.application.support.DiplomaAccessPolicy;
import ayd2.p2b.conference_service_api.feature.diploma.dto.internal.DiplomaRequesterContext;
import ayd2.p2b.conference_service_api.feature.diploma.dto.response.DiplomaPrintDataResponse;
import ayd2.p2b.conference_service_api.feature.diploma.dto.response.DiplomaResponse;
import ayd2.p2b.conference_service_api.integration.dto.IamUserSummary;
import ayd2.p2b.conference_service_api.integration.exception.IntegrationExceptions;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class GetDiplomaPrintDataUseCase {

    private final DiplomaRepositoryPort diplomaRepositoryPort;
    private final IamUserLookupPort iamUserLookupPort;

    public GetDiplomaPrintDataUseCase(
            DiplomaRepositoryPort diplomaRepositoryPort,
            IamUserLookupPort iamUserLookupPort
    ) {
        this.diplomaRepositoryPort = diplomaRepositoryPort;
        this.iamUserLookupPort = iamUserLookupPort;
    }

    public DiplomaPrintDataResponse execute(UUID diplomaId, DiplomaRequesterContext requester) {
        DiplomaAccessPolicy.ensureParticipant(requester);

        DiplomaResponse diploma = diplomaRepositoryPort.findResponseById(diplomaId)
                .orElseThrow(() -> DiplomaExceptions.diplomaNotFound(diplomaId));
        DiplomaAccessPolicy.ensureOwner(diploma.getUserId(), requester);

        String accessToken = requester.getAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            throw IntegrationExceptions.iamUnavailable("IAM access token is required to resolve diploma print data");
        }

        Map<UUID, IamUserSummary> usersSummary = iamUserLookupPort.getUsersSummary(
                Set.of(diploma.getUserId()),
                accessToken
        );
        IamUserSummary userSummary = usersSummary == null ? null : usersSummary.get(diploma.getUserId());
        String userFullName = userSummary == null ? null : userSummary.getFullName();
        if (userFullName == null || userFullName.isBlank()) {
            throw IntegrationExceptions.iamUnavailable("IAM user summary is incomplete for diploma owner");
        }

        return DiplomaPrintDataResponse.builder()
                .diplomaId(diploma.getId())
                .userId(diploma.getUserId())
                .userFullName(userFullName)
                .congressId(diploma.getCongressId())
                .congressName(diploma.getCongressName())
                .activityId(diploma.getActivityId())
                .activityName(diploma.getActivityName())
                .type(diploma.getType())
                .issuedAt(diploma.getIssuedAt())
                .build();
    }
}
