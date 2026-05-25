package ayd2.p2b.conference_service_api.feature.report.application.congresses_by_institution.port;

import ayd2.p2b.conference_service_api.feature.report.dto.response.CongressByInstitutionItem;

import java.time.LocalDate;
import java.util.List;

public interface CongressesByInstitutionQueryPort {
    List<CongressByInstitutionItem> query(LocalDate dateFrom, LocalDate dateTo);
}
