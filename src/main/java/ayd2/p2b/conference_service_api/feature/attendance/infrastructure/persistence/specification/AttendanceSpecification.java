package ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.specification;

import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.entity.ActivityEntity;
import ayd2.p2b.conference_service_api.feature.attendance.dto.internal.AttendanceSearchCriteria;
import ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.entity.AttendanceEntity;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.entity.CongressEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public final class AttendanceSpecification {

    private AttendanceSpecification() {
    }

    public static Specification<AttendanceEntity> byCriteriaAndScope(
            AttendanceSearchCriteria criteria,
            UUID requesterUserId,
            Set<UUID> linkedInstitutionIds
    ) {
        return (root, query, cb) -> {
            Join<AttendanceEntity, ActivityEntity> activityJoin = root.join("activity");
            Join<ActivityEntity, CongressEntity> congressJoin = activityJoin.join("congress");

            ArrayList<Predicate> predicates = new ArrayList<>();
            predicates.add(scopePredicate(cb, congressJoin, requesterUserId, linkedInstitutionIds));

            if (criteria != null) {
                if (criteria.getActivityId() != null) {
                    predicates.add(cb.equal(root.get("activityId"), criteria.getActivityId()));
                }
                if (criteria.getRoomId() != null) {
                    predicates.add(cb.equal(activityJoin.get("roomId"), criteria.getRoomId()));
                }
                if (criteria.getDateFrom() != null) {
                    OffsetDateTime from = criteria.getDateFrom().atStartOfDay().atOffset(ZoneOffset.UTC);
                    predicates.add(cb.greaterThanOrEqualTo(root.get("registeredAt"), from));
                }
                if (criteria.getDateTo() != null) {
                    OffsetDateTime until = criteria.getDateTo().plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
                    predicates.add(cb.lessThan(root.get("registeredAt"), until));
                }
                if (criteria.getPersonalId() != null && !criteria.getPersonalId().isBlank()) {
                    predicates.add(cb.equal(
                            cb.lower(root.get("personalIdSnapshot")),
                            criteria.getPersonalId().trim().toLowerCase()
                    ));
                }
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate scopePredicate(
            jakarta.persistence.criteria.CriteriaBuilder cb,
            Join<ActivityEntity, CongressEntity> congressJoin,
            UUID requesterUserId,
            Set<UUID> linkedInstitutionIds
    ) {
        Predicate ownerPredicate = cb.equal(congressJoin.get("createdBy"), requesterUserId);
        if (linkedInstitutionIds == null || linkedInstitutionIds.isEmpty()) {
            return ownerPredicate;
        }
        Predicate linkedPredicate = congressJoin.get("institutionId").in(linkedInstitutionIds);
        return cb.or(ownerPredicate, linkedPredicate);
    }
}
