package ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.specification;

import ayd2.p2b.conference_service_api.feature.activity.dto.internal.ActivitySearchCriteria;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.entity.ActivityEntity;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.entity.CongressEntity;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.entity.InstitutionEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ActivitySpecification {

    private ActivitySpecification() {
    }

    public static Specification<ActivityEntity> publicByCongressAndFilters(UUID congressId, ActivitySearchCriteria criteria) {
        return (root, query, cb) -> {
            Join<ActivityEntity, CongressEntity> congressJoin = root.join("congress");
            Join<CongressEntity, InstitutionEntity> institutionJoin = congressJoin.join("institution");
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("congressId"), congressId));
            predicates.add(cb.isTrue(institutionJoin.get("active")));

            if (criteria != null) {
                if (criteria.getRoomId() != null) {
                    predicates.add(cb.equal(root.get("roomId"), criteria.getRoomId()));
                }
                if (criteria.getType() != null) {
                    predicates.add(cb.equal(root.get("type"), criteria.getType()));
                }
                if (criteria.getDateFrom() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), criteria.getDateFrom()));
                }
                if (criteria.getDateTo() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("startTime"), criteria.getDateTo()));
                }
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
