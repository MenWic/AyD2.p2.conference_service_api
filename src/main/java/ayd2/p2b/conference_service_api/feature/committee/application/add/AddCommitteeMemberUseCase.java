package ayd2.p2b.conference_service_api.feature.committee.application.add;

import ayd2.p2b.conference_service_api.feature.committee.application.exception.CommitteeExceptions;
import ayd2.p2b.conference_service_api.feature.committee.application.port.CommitteeCongressPort;
import ayd2.p2b.conference_service_api.feature.committee.application.port.CommitteeRepositoryPort;
import ayd2.p2b.conference_service_api.feature.committee.application.support.CommitteeAccessPolicy;
import ayd2.p2b.conference_service_api.feature.committee.domain.model.CommitteeMember;
import ayd2.p2b.conference_service_api.feature.committee.dto.internal.CommitteeCongressSummary;
import ayd2.p2b.conference_service_api.feature.committee.dto.internal.CommitteeRequesterContext;
import ayd2.p2b.conference_service_api.feature.committee.dto.request.AddCommitteeMemberRequest;
import ayd2.p2b.conference_service_api.feature.committee.dto.response.CommitteeMemberResponse;
import ayd2.p2b.conference_service_api.feature.committee.mapper.CommitteeMapper;
import ayd2.p2b.conference_service_api.integration.dto.IamCommitteeCandidateSummary;
import ayd2.p2b.conference_service_api.integration.exception.IntegrationExceptions;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Transactional
public class AddCommitteeMemberUseCase {

    private final CommitteeRepositoryPort committeeRepositoryPort;
    private final CommitteeCongressPort committeeCongressPort;
    private final IamUserLookupPort iamUserLookupPort;
    private final CommitteeMapper committeeMapper;

    public AddCommitteeMemberUseCase(
            CommitteeRepositoryPort committeeRepositoryPort,
            CommitteeCongressPort committeeCongressPort,
            IamUserLookupPort iamUserLookupPort,
            CommitteeMapper committeeMapper
    ) {
        this.committeeRepositoryPort = committeeRepositoryPort;
        this.committeeCongressPort = committeeCongressPort;
        this.iamUserLookupPort = iamUserLookupPort;
        this.committeeMapper = committeeMapper;
    }

    public CommitteeMemberResponse execute(
            UUID congressId,
            AddCommitteeMemberRequest request,
            CommitteeRequesterContext requester
    ) {
        CommitteeAccessPolicy.ensureCongressAdminWrite(requester);
        UUID userId = requiredUserId(request);

        CommitteeCongressSummary congress = committeeCongressPort.findManageableCongressById(congressId)
                .orElseThrow(() -> CommitteeExceptions.congressNotFound(congressId));
        authorizeScopedAccess(requester, congress);

        if (committeeRepositoryPort.existsByCongressIdAndUserId(congressId, userId)) {
            throw CommitteeExceptions.memberAlreadyExists(congressId, userId);
        }

        IamCommitteeCandidateSummary candidateSummary = iamUserLookupPort.getCommitteeCandidateSummary(
                userId,
                requester.getAccessToken()
        );
        if (!candidateSummary.isEligible()) {
            throw CommitteeExceptions.candidateNotEligible(userId);
        }
        ensureCandidateProfile(candidateSummary, userId);

        CommitteeMember member = CommitteeMember.builder()
                .congressId(congressId)
                .userId(userId)
                .addedBy(requester.getUserId())
                .build();
        CommitteeMember saved = committeeRepositoryPort.save(member);
        return committeeMapper.toResponse(saved, candidateSummary);
    }

    private UUID requiredUserId(AddCommitteeMemberRequest request) {
        if (request == null || request.getUserId() == null) {
            throw CommitteeExceptions.validationFailed("userId is required");
        }
        return request.getUserId();
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

    private void ensureCandidateProfile(IamCommitteeCandidateSummary summary, UUID userId) {
        if (summary.getUserId() == null
                || !userId.equals(summary.getUserId())
                || summary.getFullName() == null
                || summary.getFullName().isBlank()
                || summary.getEmail() == null
                || summary.getEmail().isBlank()) {
            throw IntegrationExceptions.iamUnavailable("IAM profile data unavailable for user: " + userId);
        }
    }
}
