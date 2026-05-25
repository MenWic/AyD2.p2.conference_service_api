package ayd2.p2b.conference_service_api.feature.report.application.participants;

import ayd2.p2b.conference_service_api.feature.report.application.exception.ReportExceptions;
import ayd2.p2b.conference_service_api.feature.report.application.participants.port.ParticipantsCongressScopePort;
import ayd2.p2b.conference_service_api.feature.report.application.participants.port.ParticipantsReportQueryPort;
import ayd2.p2b.conference_service_api.feature.report.application.support.ReportAccessPolicy;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.CongressInstitutionSummary;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ParticipantRow;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ReportRequesterContext;
import ayd2.p2b.conference_service_api.feature.report.dto.response.ParticipantItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.ParticipantsReportResponse;
import ayd2.p2b.conference_service_api.feature.report.dto.response.ParticipationTypeEnum;
import ayd2.p2b.conference_service_api.integration.dto.IamUserDetailSummary;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Transactional(readOnly = true)
public class ParticipantsReportUseCase {

    private final ParticipantsCongressScopePort congressScopePort;
    private final ParticipantsReportQueryPort queryPort;
    private final IamUserLookupPort iamUserLookupPort;

    public ParticipantsReportUseCase(
            ParticipantsCongressScopePort congressScopePort,
            ParticipantsReportQueryPort queryPort,
            IamUserLookupPort iamUserLookupPort) {
        this.congressScopePort = congressScopePort;
        this.queryPort = queryPort;
        this.iamUserLookupPort = iamUserLookupPort;
    }

    public ParticipantsReportResponse execute(UUID congressId, ParticipationTypeEnum typeFilter,
                                               ReportRequesterContext requester) {
        ReportAccessPolicy.ensureCongressAdmin(requester);

        if (congressId == null) {
            throw ReportExceptions.missingCongressId();
        }

        CongressInstitutionSummary summary = congressScopePort.findCongressSummary(congressId)
                .orElseThrow(() -> ReportExceptions.congressNotFound(congressId));

        if (!iamUserLookupPort.isCongressAdminLinkedToInstitution(
                requester.getUserId(), summary.getInstitutionId(), requester.getAccessToken())) {
            throw ReportExceptions.congressAccessDenied(congressId);
        }

        List<ParticipantRow> rows = queryPort.findParticipants(congressId);

        List<ParticipantRow> filtered = typeFilter == null
                ? rows
                : rows.stream()
                        .filter(r -> r.getParticipationTypes() != null && r.getParticipationTypes().contains(typeFilter))
                        .toList();

        if (filtered.isEmpty()) {
            return ParticipantsReportResponse.builder()
                    .items(List.of())
                    .totalItems(0)
                    .build();
        }

        Set<UUID> userIds = filtered.stream().map(ParticipantRow::getUserId).collect(Collectors.toSet());
        Map<UUID, IamUserDetailSummary> detailMap = iamUserLookupPort.getUserDetailsSummary(
                userIds, requester.getAccessToken());

        List<ParticipantItem> items = new ArrayList<>();
        for (ParticipantRow row : filtered) {
            IamUserDetailSummary detail = detailMap.get(row.getUserId());
            ParticipantItem item = buildItem(row, detail);
            items.add(item);
        }

        return ParticipantsReportResponse.builder()
                .items(items)
                .totalItems(items.size())
                .build();
    }

    private ParticipantItem buildItem(ParticipantRow row, IamUserDetailSummary detail) {
        ParticipantItem.ParticipantItemBuilder builder = ParticipantItem.builder()
                .participationTypes(row.getParticipationTypes() == null
                        ? List.of()
                        : new ArrayList<>(row.getParticipationTypes()));
        if (detail != null) {
            builder.personalId(detail.getPersonalId())
                    .fullName(detail.getFullName())
                    .organization(detail.getOrganization())
                    .email(detail.getEmail())
                    .phone(detail.getPhone());
        }
        return builder.build();
    }
}
