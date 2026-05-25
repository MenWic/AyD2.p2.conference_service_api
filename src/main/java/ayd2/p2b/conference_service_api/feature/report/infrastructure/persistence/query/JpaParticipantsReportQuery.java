package ayd2.p2b.conference_service_api.feature.report.infrastructure.persistence.query;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeaderType;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.repository.ActivityLeaderRepository;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.repository.ActivityRepository;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.repository.EnrollmentJpaRepository;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalStatus;
import ayd2.p2b.conference_service_api.feature.proposal.infrastructure.persistence.repository.ProposalRepository;
import ayd2.p2b.conference_service_api.feature.report.application.participants.port.ParticipantsReportQueryPort;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ParticipantRow;
import ayd2.p2b.conference_service_api.feature.report.dto.response.ParticipationTypeEnum;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JpaParticipantsReportQuery implements ParticipantsReportQueryPort {

    private final EnrollmentJpaRepository enrollmentRepository;
    private final ProposalRepository proposalRepository;
    private final ActivityLeaderRepository activityLeaderRepository;
    private final ActivityRepository activityRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<ParticipantRow> findParticipants(UUID congressId) {
        Map<UUID, EnumSet<ParticipationTypeEnum>> participantMap = new HashMap<>();

        // 1. Enrolled users
        enrollmentRepository.findByCongressId(congressId, Pageable.unpaged()).getContent()
                .forEach(e -> merge(participantMap, e.getUserId(), ParticipationTypeEnum.ENROLLED));

        // 2. Approved proposal authors via calls of this congress (JPQL join)
        List<UUID> approvedAuthorIds = entityManager.createQuery(
                        """
                        select p.authorUserId
                        from ProposalEntity p
                        join p.call c
                        where c.congressId = :congressId
                          and p.status = :status
                        """, UUID.class)
                .setParameter("congressId", congressId)
                .setParameter("status", ProposalStatus.APPROVED)
                .getResultList();
        approvedAuthorIds.forEach(uid -> merge(participantMap, uid, ParticipationTypeEnum.PROPOSAL_AUTHOR));

        // 3. Activity leaders of activities in this congress
        Set<UUID> activityIds = activityRepository.findAll().stream()
                .filter(a -> congressId.equals(a.getCongressId()))
                .map(a -> a.getId())
                .collect(Collectors.toSet());

        if (!activityIds.isEmpty()) {
            activityLeaderRepository.findByActivityIdInOrderByActivityIdAscUserIdAsc(activityIds)
                    .forEach(leader -> {
                        ParticipationTypeEnum type = mapLeaderType(leader.getLeaderType());
                        if (type != null) {
                            merge(participantMap, leader.getUserId(), type);
                        }
                    });
        }

        List<ParticipantRow> rows = new ArrayList<>();
        for (Map.Entry<UUID, EnumSet<ParticipationTypeEnum>> entry : participantMap.entrySet()) {
            rows.add(ParticipantRow.builder()
                    .userId(entry.getKey())
                    .participationTypes(entry.getValue())
                    .build());
        }
        return rows;
    }

    private void merge(Map<UUID, EnumSet<ParticipationTypeEnum>> map, UUID userId, ParticipationTypeEnum type) {
        map.computeIfAbsent(userId, k -> EnumSet.noneOf(ParticipationTypeEnum.class)).add(type);
    }

    private ParticipationTypeEnum mapLeaderType(ActivityLeaderType leaderType) {
        if (leaderType == null) {
            return null;
        }
        return switch (leaderType) {
            case SPEAKER -> ParticipationTypeEnum.SPEAKER;
            case WORKSHOP_LEADER -> ParticipationTypeEnum.WORKSHOP_LEADER;
            case GUEST_SPEAKER -> ParticipationTypeEnum.GUEST_SPEAKER;
        };
    }
}
