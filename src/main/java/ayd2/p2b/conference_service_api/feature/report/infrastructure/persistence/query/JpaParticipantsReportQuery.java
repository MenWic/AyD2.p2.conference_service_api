package ayd2.p2b.conference_service_api.feature.report.infrastructure.persistence.query;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeaderType;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.repository.EnrollmentJpaRepository;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalStatus;
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
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaParticipantsReportQuery implements ParticipantsReportQueryPort {

    private final EnrollmentJpaRepository enrollmentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<ParticipantRow> findParticipants(UUID congressId) {
        Map<UUID, EnumSet<ParticipationTypeEnum>> participantMap = new HashMap<>();

        enrollmentRepository.findByCongressId(congressId, Pageable.unpaged()).getContent()
                .forEach(enrollment -> merge(participantMap, enrollment.getUserId(), ParticipationTypeEnum.ENROLLED));

        entityManager.createQuery(
                        """
                        select p.authorUserId
                        from ProposalEntity p
                        join p.call c
                        where c.congressId = :congressId
                          and p.status = :status
                        """,
                        UUID.class)
                .setParameter("congressId", congressId)
                .setParameter("status", ProposalStatus.APPROVED)
                .getResultList()
                .forEach(authorUserId -> merge(participantMap, authorUserId, ParticipationTypeEnum.PROPOSAL_AUTHOR));

        entityManager.createQuery(
                        """
                        select al.userId, al.leaderType
                        from ActivityLeaderEntity al
                        join al.activity a
                        where a.congressId = :congressId
                        """,
                        Object[].class)
                .setParameter("congressId", congressId)
                .getResultList()
                .forEach(row -> {
                    UUID userId = (UUID) row[0];
                    ActivityLeaderType leaderType = (ActivityLeaderType) row[1];
                    ParticipationTypeEnum mappedType = mapLeaderType(leaderType);
                    if (mappedType != null) {
                        merge(participantMap, userId, mappedType);
                    }
                });

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
        map.computeIfAbsent(userId, ignored -> EnumSet.noneOf(ParticipationTypeEnum.class)).add(type);
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
