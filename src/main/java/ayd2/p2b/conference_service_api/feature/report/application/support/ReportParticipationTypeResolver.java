package ayd2.p2b.conference_service_api.feature.report.application.support;

import ayd2.p2b.conference_service_api.feature.report.dto.response.ParticipationTypeEnum;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;

@Component
public class ReportParticipationTypeResolver {

    private static final Map<ParticipationTypeEnum, Integer> PRIORITY = Map.of(
            ParticipationTypeEnum.SPEAKER, 1,
            ParticipationTypeEnum.WORKSHOP_LEADER, 2,
            ParticipationTypeEnum.GUEST_SPEAKER, 3,
            ParticipationTypeEnum.PROPOSAL_AUTHOR, 4,
            ParticipationTypeEnum.ENROLLED, 5
    );

    public ParticipationTypeEnum resolvePrimaryType(Set<ParticipationTypeEnum> participationTypes) {
        if (participationTypes == null || participationTypes.isEmpty()) {
            return ParticipationTypeEnum.ENROLLED;
        }
        return participationTypes.stream()
                .min(Comparator.comparingInt(type -> PRIORITY.getOrDefault(type, Integer.MAX_VALUE)))
                .orElse(ParticipationTypeEnum.ENROLLED);
    }
}
