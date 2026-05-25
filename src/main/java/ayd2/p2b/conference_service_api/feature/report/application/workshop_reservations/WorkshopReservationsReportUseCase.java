package ayd2.p2b.conference_service_api.feature.report.application.workshop_reservations;

import ayd2.p2b.conference_service_api.feature.report.application.exception.ReportExceptions;
import ayd2.p2b.conference_service_api.feature.report.application.participants.port.ParticipantsCongressScopePort;
import ayd2.p2b.conference_service_api.feature.report.application.support.ReportAccessPolicy;
import ayd2.p2b.conference_service_api.feature.report.application.workshop_reservations.port.WorkshopReservationsQueryPort;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.CongressInstitutionSummary;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ReportRequesterContext;
import ayd2.p2b.conference_service_api.feature.report.dto.response.WorkshopReservationItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.WorkshopReservationsReportResponse;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class WorkshopReservationsReportUseCase {

    private final ParticipantsCongressScopePort congressScopePort;
    private final WorkshopReservationsQueryPort queryPort;
    private final IamUserLookupPort iamUserLookupPort;

    public WorkshopReservationsReportUseCase(
            ParticipantsCongressScopePort congressScopePort,
            WorkshopReservationsQueryPort queryPort,
            IamUserLookupPort iamUserLookupPort) {
        this.congressScopePort = congressScopePort;
        this.queryPort = queryPort;
        this.iamUserLookupPort = iamUserLookupPort;
    }

    public WorkshopReservationsReportResponse execute(UUID congressId, UUID activityIdFilter,
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

        List<WorkshopReservationItem> items = queryPort.query(congressId, activityIdFilter);

        return WorkshopReservationsReportResponse.builder()
                .items(items)
                .totalItems(items.size())
                .build();
    }
}
