package ayd2.p2b.conference_service_api.feature.committee.application.remove;

import ayd2.p2b.conference_service_api.feature.committee.application.exception.CommitteeExceptions;
import ayd2.p2b.conference_service_api.feature.committee.application.port.CommitteeCongressPort;
import ayd2.p2b.conference_service_api.feature.committee.application.port.CommitteeRepositoryPort;
import ayd2.p2b.conference_service_api.feature.committee.application.support.CommitteeAccessPolicy;
import ayd2.p2b.conference_service_api.feature.committee.dto.internal.CommitteeCongressSummary;
import ayd2.p2b.conference_service_api.feature.committee.dto.internal.CommitteeRequesterContext;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Transactional
public class RemoveCommitteeMemberUseCase {

    private final CommitteeRepositoryPort committeeRepositoryPort;
    private final CommitteeCongressPort committeeCongressPort;
    private final IamUserLookupPort iamUserLookupPort;

    public RemoveCommitteeMemberUseCase(
            CommitteeRepositoryPort committeeRepositoryPort,
            CommitteeCongressPort committeeCongressPort,
            IamUserLookupPort iamUserLookupPort
    ) {
        this.committeeRepositoryPort = committeeRepositoryPort;
        this.committeeCongressPort = committeeCongressPort;
        this.iamUserLookupPort = iamUserLookupPort;
    }

    public void execute(UUID congressId, UUID userId, CommitteeRequesterContext requester) {
        CommitteeAccessPolicy.ensureCongressAdminWrite(requester);

        CommitteeCongressSummary congress = committeeCongressPort.findManageableCongressById(congressId)
                .orElseThrow(() -> CommitteeExceptions.congressNotFound(congressId));
        authorizeScopedAccess(requester, congress);

        if (!committeeRepositoryPort.existsByCongressIdAndUserId(congressId, userId)) {
            throw CommitteeExceptions.memberNotFound(congressId, userId);
        }

        committeeRepositoryPort.deleteByCongressIdAndUserId(congressId, userId);
    }

    private void authorizeScopedAccess(CommitteeRequesterContext requester, CommitteeCongressSummary congress) {
        if (requester.getUserId().equals(congress.getCreatedBy())) {
            CommitteeAccessPolicy.ensureCanManageCongress(requester, congress, false);
            return;
        }
        boolean linkedToInstitution = iamUserLookupPort.isCongressAdminLinkedToInstitution(
                requester.getUserId(),
                congress.getInstitutionId(),
                requester.getAccessToken()
        );
        CommitteeAccessPolicy.ensureCanManageCongress(requester, congress, linkedToInstitution);
    }
}
