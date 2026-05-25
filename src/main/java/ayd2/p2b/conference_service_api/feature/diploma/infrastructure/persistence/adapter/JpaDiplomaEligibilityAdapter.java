package ayd2.p2b.conference_service_api.feature.diploma.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.diploma.application.port.DiplomaEligibilityPort;
import ayd2.p2b.conference_service_api.feature.diploma.domain.model.DiplomaType;
import ayd2.p2b.conference_service_api.feature.diploma.dto.internal.DiplomaEligibilityCandidate;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class JpaDiplomaEligibilityAdapter implements DiplomaEligibilityPort {

    private final EntityManager entityManager;

    public JpaDiplomaEligibilityAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<DiplomaEligibilityCandidate> findParticipationCandidates(UUID userId) {
        Query query = entityManager.createNativeQuery("""
                select at.user_id, a.congress_id, c.name
                from attendances at
                join activities a on a.id = at.activity_id
                join congresses c on c.id = a.congress_id
                where at.user_id = :userId
                group by at.user_id, a.congress_id, c.name
                having count(*) >= 3
                """);
        query.setParameter("userId", userId);
        return toParticipationCandidates(query.getResultList());
    }

    @Override
    public List<DiplomaEligibilityCandidate> findLeadershipCandidates(UUID userId) {
        Query query = entityManager.createNativeQuery("""
                select al.user_id, a.congress_id, c.name, a.id, a.name
                from activity_leaders al
                join activities a on a.id = al.activity_id
                join congresses c on c.id = a.congress_id
                where al.user_id = :userId
                """);
        query.setParameter("userId", userId);
        return toLeadershipCandidates(query.getResultList());
    }

    @SuppressWarnings("unchecked")
    private List<DiplomaEligibilityCandidate> toParticipationCandidates(List<?> rows) {
        return rows.stream()
                .map(row -> (Object[]) row)
                .map(row -> DiplomaEligibilityCandidate.builder()
                        .userId((UUID) row[0])
                        .congressId((UUID) row[1])
                        .congressName((String) row[2])
                        .type(DiplomaType.PARTICIPATION)
                        .activityId(null)
                        .activityName(null)
                        .build())
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<DiplomaEligibilityCandidate> toLeadershipCandidates(List<?> rows) {
        return rows.stream()
                .map(row -> (Object[]) row)
                .map(row -> DiplomaEligibilityCandidate.builder()
                        .userId((UUID) row[0])
                        .congressId((UUID) row[1])
                        .congressName((String) row[2])
                        .type(DiplomaType.LEADERSHIP)
                        .activityId((UUID) row[3])
                        .activityName((String) row[4])
                        .build())
                .toList();
    }
}
