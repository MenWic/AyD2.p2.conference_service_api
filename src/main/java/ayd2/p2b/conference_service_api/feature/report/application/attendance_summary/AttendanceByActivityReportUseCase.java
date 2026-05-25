package ayd2.p2b.conference_service_api.feature.report.application.attendance_summary;

import ayd2.p2b.conference_service_api.feature.report.application.attendance_summary.port.AttendanceByActivityQueryPort;
import ayd2.p2b.conference_service_api.feature.report.application.exception.ReportExceptions;
import ayd2.p2b.conference_service_api.feature.report.application.participants.port.ParticipantsCongressScopePort;
import ayd2.p2b.conference_service_api.feature.report.application.support.ReportAccessPolicy;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.CongressInstitutionSummary;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ReportRequesterContext;
import ayd2.p2b.conference_service_api.feature.report.dto.response.AttendanceActivityItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.AttendanceByActivityReportResponse;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class AttendanceByActivityReportUseCase {

    private final ParticipantsCongressScopePort congressScopePort;
    private final AttendanceByActivityQueryPort queryPort;
    private final IamUserLookupPort iamUserLookupPort;

    public AttendanceByActivityReportUseCase(
            ParticipantsCongressScopePort congressScopePort,
            AttendanceByActivityQueryPort queryPort,
            IamUserLookupPort iamUserLookupPort) {
        this.congressScopePort = congressScopePort;
        this.queryPort = queryPort;
        this.iamUserLookupPort = iamUserLookupPort;
    }

    public AttendanceByActivityReportResponse execute(UUID congressId, UUID activityIdFilter,
                                                       UUID roomIdFilter, OffsetDateTime dateFrom,
                                                       OffsetDateTime dateTo, ReportRequesterContext requester) {
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

        List<AttendanceActivityItem> items = queryPort.query(congressId, activityIdFilter, roomIdFilter, dateFrom, dateTo);

        return AttendanceByActivityReportResponse.builder()
                .items(items)
                .totalItems(items.size())
                .build();
    }
}
