package ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.specification;

import ayd2.p2b.conference_service_api.feature.congress.dto.internal.CongressSearchCriteria;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.entity.CongressEntity;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.entity.InstitutionEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class CongressSpecification {

    private CongressSpecification() {
    }

    public static Specification<CongressEntity> publicCriteria(CongressSearchCriteria criteria) {
        return (root, query, cb) -> {
            Join<CongressEntity, InstitutionEntity> institution = root.join("institution");
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(institution.get("active")));

            if (criteria != null) {
                if (criteria.getInstitutionId() != null) {
                    predicates.add(cb.equal(root.get("institutionId"), criteria.getInstitutionId()));
                }
                if (criteria.getStartDateFrom() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), criteria.getStartDateFrom()));
                }
                if (criteria.getStartDateTo() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), criteria.getStartDateTo()));
                }
                if (criteria.getSearch() != null && !criteria.getSearch().isBlank()) {
                    String pattern = "%" + criteria.getSearch().toLowerCase() + "%";
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("name")), pattern),
                            cb.like(cb.lower(root.get("description")), pattern),
                            cb.like(cb.lower(root.get("location")), pattern)
                    ));
                }
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
