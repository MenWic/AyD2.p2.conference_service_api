package ayd2.p2b.conference_service_api.feature.attendance.application.list;

import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.attendance.application.port.AttendanceRepositoryPort;
import ayd2.p2b.conference_service_api.feature.attendance.application.support.AttendanceAccessPolicy;
import ayd2.p2b.conference_service_api.feature.attendance.dto.internal.AttendanceRequesterContext;
import ayd2.p2b.conference_service_api.feature.attendance.dto.internal.AttendanceSearchCriteria;
import ayd2.p2b.conference_service_api.feature.attendance.dto.response.AttendanceResponse;
import ayd2.p2b.conference_service_api.feature.attendance.mapper.AttendanceMapper;
import ayd2.p2b.conference_service_api.integration.dto.IamUserSummary;
import ayd2.p2b.conference_service_api.integration.exception.IntegrationExceptions;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class ListAttendanceUseCase {

    private final AttendanceRepositoryPort attendanceRepositoryPort;
    private final IamUserLookupPort iamUserLookupPort;
    private final AttendanceMapper attendanceMapper;

    public ListAttendanceUseCase(
            AttendanceRepositoryPort attendanceRepositoryPort,
            IamUserLookupPort iamUserLookupPort,
            AttendanceMapper attendanceMapper
    ) {
        this.attendanceRepositoryPort = attendanceRepositoryPort;
        this.iamUserLookupPort = iamUserLookupPort;
        this.attendanceMapper = attendanceMapper;
    }

    public PageResponse<AttendanceResponse> execute(
            AttendanceSearchCriteria criteria,
            Pageable pageable,
            AttendanceRequesterContext requester
    ) {
        AttendanceAccessPolicy.ensureCongressAdminScoped(requester);
        Set<UUID> linkedInstitutions = resolveLinkedInstitutions(requester);

        Page<AttendanceResponse> page = attendanceRepositoryPort
                .findByCriteria(criteria, pageable, requester.getUserId(), linkedInstitutions)
                .map(attendanceMapper::toResponse);

        return PageResponse.<AttendanceResponse>builder()
                .items(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    private Set<UUID> resolveLinkedInstitutions(AttendanceRequesterContext requester) {
        IamUserSummary summary = iamUserLookupPort.getUsersSummary(Set.of(requester.getUserId()), requester.getAccessToken())
                .get(requester.getUserId());
        if (summary == null) {
            throw IntegrationExceptions.iamUnavailable("Requester IAM summary is unavailable");
        }
        return summary.getLinkedInstitutions() == null ? Set.of() : summary.getLinkedInstitutions();
    }
}
