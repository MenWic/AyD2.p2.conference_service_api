package ayd2.p2b.conference_service_api.feature.report.infrastructure.persistence.query;

import ayd2.p2b.conference_service_api.feature.report.application.congresses_by_institution.port.CongressesByInstitutionQueryPort;
import ayd2.p2b.conference_service_api.feature.report.dto.response.CongressByInstitutionItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaCongressesByInstitutionQuery implements CongressesByInstitutionQueryPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<CongressByInstitutionItem> query(LocalDate dateFrom, LocalDate dateTo) {
        StringBuilder jpql = new StringBuilder("""
                select c.id, c.institutionId, i.name, c.name, c.startDate, c.endDate, c.location, c.price
                from CongressEntity c
                join c.institution i
                where 1=1
                """);

        if (dateFrom != null) {
            jpql.append(" and c.startDate >= :dateFrom");
        }
        if (dateTo != null) {
            jpql.append(" and c.startDate <= :dateTo");
        }
        jpql.append(" order by i.name asc, c.startDate asc, c.name asc");

        var q = entityManager.createQuery(jpql.toString(), Object[].class);

        if (dateFrom != null) {
            q.setParameter("dateFrom", dateFrom);
        }
        if (dateTo != null) {
            q.setParameter("dateTo", dateTo);
        }

        return q.getResultList().stream()
                .map(row -> CongressByInstitutionItem.builder()
                        .congressId((UUID) row[0])
                        .institutionId((UUID) row[1])
                        .institutionName((String) row[2])
                        .congressName((String) row[3])
                        .startDate((LocalDate) row[4])
                        .endDate((LocalDate) row[5])
                        .location((String) row[6])
                        .price((BigDecimal) row[7])
                        .build())
                .toList();
    }
}
