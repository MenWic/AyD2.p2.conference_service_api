package ayd2.p2b.conference_service_api.feature.report.application.congresses_by_institution;

import ayd2.p2b.conference_service_api.feature.report.application.congresses_by_institution.port.CongressesByInstitutionQueryPort;
import ayd2.p2b.conference_service_api.feature.report.application.support.ReportAccessPolicy;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ReportRequesterContext;
import ayd2.p2b.conference_service_api.feature.report.dto.response.CongressByInstitutionItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.CongressesByInstitutionReportResponse;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@Transactional(readOnly = true)
public class CongressesByInstitutionReportUseCase {

    private final CongressesByInstitutionQueryPort queryPort;

    public CongressesByInstitutionReportUseCase(CongressesByInstitutionQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public CongressesByInstitutionReportResponse execute(LocalDate dateFrom, LocalDate dateTo,
                                                          ReportRequesterContext requester) {
        ReportAccessPolicy.ensureSystemAdmin(requester);

        List<CongressByInstitutionItem> items = queryPort.query(dateFrom, dateTo);

        return CongressesByInstitutionReportResponse.builder()
                .items(items)
                .totalItems(items.size())
                .build();
    }
}
