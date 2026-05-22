package ayd2.p2b.conference_service_api.feature.committee.application.list;

import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.committee.application.exception.CommitteeExceptions;
import ayd2.p2b.conference_service_api.feature.committee.application.port.CommitteeCongressPort;
import ayd2.p2b.conference_service_api.feature.committee.application.port.CommitteeRepositoryPort;
import ayd2.p2b.conference_service_api.feature.committee.application.support.CommitteeAccessPolicy;
import ayd2.p2b.conference_service_api.feature.committee.domain.model.CommitteeMember;
import ayd2.p2b.conference_service_api.feature.committee.dto.internal.CommitteeCongressSummary;
import ayd2.p2b.conference_service_api.feature.committee.dto.internal.CommitteeRequesterContext;
import ayd2.p2b.conference_service_api.feature.committee.dto.response.CommitteeMemberResponse;
import ayd2.p2b.conference_service_api.feature.committee.mapper.CommitteeMapper;
import ayd2.p2b.conference_service_api.integration.dto.IamUserSummary;
import ayd2.p2b.conference_service_api.integration.exception.IntegrationExceptions;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Transactional(readOnly = true)
public class ListCommitteeMembersUseCase {

    private final CommitteeRepositoryPort committeeRepositoryPort;
    private final CommitteeCongressPort committeeCongressPort;
    private final IamUserLookupPort iamUserLookupPort;
    private final CommitteeMapper committeeMapper;

    public ListCommitteeMembersUseCase(
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

    public PageResponse<CommitteeMemberResponse> execute(
            UUID congressId,
            Pageable pageable,
            CommitteeRequesterContext requester
    ) {
        CommitteeAccessPolicy.ensureReadRole(requester);

        CommitteeCongressSummary congress = committeeCongressPort.findManageableCongressById(congressId)
                .orElseThrow(() -> CommitteeExceptions.congressNotFound(congressId));
        authorizeReadAccess(requester, congress);

        Page<CommitteeMember> page = committeeRepositoryPort.findByCongressId(congressId, pageable);
        Set<UUID> userIds = page.getContent().stream().map(CommitteeMember::getUserId).collect(Collectors.toSet());
        Map<UUID, IamUserSummary> summaries = iamUserLookupPort.getUsersSummary(userIds, requester.getAccessToken());

        Page<CommitteeMemberResponse> mapped = page.map(member -> {
            IamUserSummary summary = summaries.get(member.getUserId());
            if (summary == null || summary.getFullName() == null || summary.getEmail() == null) {
                throw IntegrationExceptions.iamUnavailable(
                        "IAM profile data unavailable for committee member: " + member.getUserId()
                );
            }
            return committeeMapper.toResponse(member, summary);
        });

        return PageResponse.<CommitteeMemberResponse>builder()
                .items(mapped.getContent())
                .page(mapped.getNumber())
                .size(mapped.getSize())
                .totalItems(mapped.getTotalElements())
                .totalPages(mapped.getTotalPages())
                .build();
    }

    private void authorizeReadAccess(CommitteeRequesterContext requester, CommitteeCongressSummary congress) {
        if (CommitteeAccessPolicy.isSystemAdmin(requester)) {
            return;
        }
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
